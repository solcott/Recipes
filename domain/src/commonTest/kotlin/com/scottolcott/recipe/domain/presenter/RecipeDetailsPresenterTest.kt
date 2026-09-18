package com.scottolcott.recipe.domain.presenter

import com.scottolcott.recipe.model.Recipe
import com.scottolcott.recipe.model.RecipeId
import com.scottolcott.recipe.repository.RecipeRepository
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import de.infix.testBalloon.framework.core.testSuite
import io.github.solcott.dataresult.DataError
import io.github.solcott.dataresult.Origin
import io.github.solcott.dataresult.Outcome
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Clock
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

private class FakeRecipeDetailsRepository : RecipeRepository {
  // Driven emission by emission: a flow that emits several values up front collapses into a single
  // recomposition, which would hide the transitions these tests are about.
  private val responses = MutableSharedFlow<Outcome<Recipe?>>(replay = 1)
  private var stored: Recipe? = null

  var getByIdHandler: () -> Flow<Outcome<Recipe?>> = { responses }

  /** Counts how many times the recipe flow was *collected*, not how many times it was built. */
  var subscriptions = 0
    private set

  /** Every favorite write, in order: `true` for an add, `false` for a remove. */
  val favoriteWrites = mutableListOf<Boolean>()

  suspend fun emit(outcome: Outcome<Recipe?>) {
    if (outcome is Outcome.Data) stored = outcome.data
    responses.emit(outcome)
  }

  override fun getById(id: RecipeId): Flow<Outcome<Recipe?>> = flow {
    subscriptions++
    emitAll(getByIdHandler())
  }

  // Room re-emits the row after a favorite write, so this does too.
  override suspend fun addFavorite(id: RecipeId) = writeFavorite(true)

  override suspend fun removeFavorite(id: RecipeId) = writeFavorite(false)

  private suspend fun writeFavorite(favorite: Boolean) {
    favoriteWrites += favorite
    stored?.let { emit(Outcome.Data(it.copy(favorite = favorite), Origin.Cache)) }
  }

  override fun recipesByIngredients(
    ingredients: Set<String>
  ): Flow<Outcome<ImmutableList<Recipe>>> = emptyFlow()

  override fun searchRecipes(query: String): Flow<Outcome<ImmutableList<Recipe>>> = emptyFlow()

  override fun recipesByCategory(category: String): Flow<Outcome<ImmutableList<Recipe>>> =
    emptyFlow()

  override fun recipesByArea(area: String): Flow<Outcome<ImmutableList<Recipe>>> = emptyFlow()

  override fun getFavoritesAsFlow(): Flow<Outcome<ImmutableList<Recipe>>> = emptyFlow()
}

private val screen = RecipeDetailsScreen(RecipeId("1"))

private fun presenterFor(repository: RecipeRepository) =
  RecipeDetailsPresenter(screen, FakeNavigator(screen), repository)

private fun recipe(favorite: Boolean = false) =
  Recipe(
    id = screen.id,
    name = "Recipe 1",
    thumbnail = "thumb1",
    category = null,
    area = null,
    favorite = favorite,
    details = null,
    lastFetched = Clock.System.now(),
  )

val recipeDetailsPresenterTests by testSuite {
  // Regression: the single-item fold once handed `hasAnswer` a non-null test where it takes an
  // `isEmpty` one, so a cached recipe counted as a miss the moment the network was asked, and the
  // screen dropped to a full-screen spinner over a recipe it already had.
  test("a cached recipe stays on screen while it refreshes") {
    val repository = FakeRecipeDetailsRepository()
    // One instance: `recipe()` stamps `lastFetched` with the current time.
    val cached = recipe()
    repository.emit(Outcome.Data(cached, Origin.Cache))

    presenterFor(repository).test {
      var state = awaitItem()
      if (state is RecipeDetailsState.Loading) state = awaitItem()
      assertIs<RecipeDetailsState.Success>(state)
      assertEquals(false, state.isRefreshing)

      repository.emit(Outcome.Loading)

      val refreshing = assertIs<RecipeDetailsState.Success>(awaitItem())
      assertEquals(true, refreshing.isRefreshing)
      assertEquals(cached, refreshing.recipe)
    }
  }

  // Regression: a settled null -- an id the API does not know, reached by a deep link -- is an
  // answer, and the fold's `checkNotNull` threw on it inside the presenter.
  test("a recipe that does not exist is an error, not a crash") {
    val repository = FakeRecipeDetailsRepository()
    repository.emit(Outcome.Data(null, Origin.Network))

    presenterFor(repository).test {
      var state = awaitItem()
      if (state is RecipeDetailsState.Loading) state = awaitItem()
      assertIs<RecipeDetailsState.Error>(state)
    }
  }

  // Regression: the success sink was remembered along with the recipe it closed over, which on the
  // first composition is always null -- so the favorite button did nothing at all.
  test("toggling favorite adds it and then removes it") {
    val repository = FakeRecipeDetailsRepository()
    repository.emit(Outcome.Data(recipe(favorite = false), Origin.Network))

    presenterFor(repository).test {
      var state = awaitItem()
      if (state is RecipeDetailsState.Loading) state = awaitItem()
      assertIs<RecipeDetailsState.Success>(state)

      state.eventSink(RecipeDetailsEvent.Success.ToggleFavorite)
      val favorited = assertIs<RecipeDetailsState.Success>(awaitItem())
      assertEquals(true, favorited.recipe.favorite)

      favorited.eventSink(RecipeDetailsEvent.Success.ToggleFavorite)
      val unfavorited = assertIs<RecipeDetailsState.Success>(awaitItem())
      assertEquals(false, unfavorited.recipe.favorite)

      assertEquals(listOf(true, false), repository.favoriteWrites)
    }
  }

  test("retry asks for the recipe again") {
    val repository = FakeRecipeDetailsRepository()
    repository.getByIdHandler = {
      if (repository.subscriptions == 1) {
        flowOf(Outcome.Error(DataError.Network, Origin.Network))
      } else {
        inFlight()
      }
    }

    presenterFor(repository).test {
      var state = awaitItem()
      if (state is RecipeDetailsState.Loading) state = awaitItem()
      assertIs<RecipeDetailsState.Error>(state)

      state.eventSink(RecipeDetailsEvent.Error.RetryClicked)

      assertIs<RecipeDetailsState.Loading>(awaitItem())
      assertEquals(2, repository.subscriptions)
    }
  }
}

/**
 * A request that is still in flight: it reports loading and then stays open. Not
 * `flowOf(Outcome.Loading)`, which completes and so reads as a settled empty result.
 */
private fun inFlight(): Flow<Outcome<Nothing>> = flow {
  emit(Outcome.Loading)
  awaitCancellation()
}
