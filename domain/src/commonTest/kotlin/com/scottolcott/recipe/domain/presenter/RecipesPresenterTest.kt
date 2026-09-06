package com.scottolcott.recipe.domain.presenter

import com.scottolcott.recipe.domain.producer.RecipesProducer
import com.scottolcott.recipe.model.Recipe
import com.scottolcott.recipe.model.RecipeId
import com.scottolcott.recipe.repository.RecipeRepository
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import org.mobilenativefoundation.store.store5.StoreReadResponse
import org.mobilenativefoundation.store.store5.StoreReadResponseOrigin

private class FakeRecipeRepository : RecipeRepository {
  val responses = MutableStateFlow<StoreReadResponse<List<Recipe>>>(StoreReadResponse.Initial)

  /** Counts how many times the ingredient flow was *collected*, not how many times it was built. */
  var ingredientSubscriptions = 0
    private set

  var lastIngredients: Set<String>? = null
    private set

  override fun recipesByIngredients(
    ingredients: Set<String>
  ): Flow<StoreReadResponse<List<Recipe>>> = flow {
    ingredientSubscriptions++
    lastIngredients = ingredients
    emitAll(responses)
  }

  override fun searchRecipes(query: String): Flow<StoreReadResponse<List<Recipe>>> = responses

  override fun recipesByCategory(category: String): Flow<StoreReadResponse<List<Recipe>>> =
    emptyFlow()

  override fun recipesByArea(area: String): Flow<StoreReadResponse<List<Recipe>>> = emptyFlow()

  override fun getById(id: RecipeId): Flow<StoreReadResponse<Recipe?>> = emptyFlow()

  override fun getFavoritesAsFlow(): Flow<StoreReadResponse<List<Recipe>>> = emptyFlow()

  override suspend fun addFavorite(id: RecipeId) = Unit

  override suspend fun removeFavorite(id: RecipeId) = Unit
}

private fun recipe(id: String) =
  Recipe(
    id = RecipeId(id),
    name = "Recipe $id",
    thumbnail = "thumb$id",
    category = null,
    area = null,
    favorite = false,
    details = null,
    lastFetched = Clock.System.now(),
  )

val recipesPresenterTests by testSuite {
  // Regression: produceByIngredients once took a `vararg`, which reaches produceRetainedState as an
  // Array. Arrays compare by identity, so the vararg call site minted a fresh key on every
  // recomposition and the collection was cancelled and restarted each pass — and since each
  // emission recomposes, it never settled.
  test("the ingredient producer subscribes once across recompositions") {
    val screen = RecipesScreen.ByIngredient(setOf("chicken", "rice"))
    val repository = FakeRecipeRepository()
    val presenter = RecipesPresenter(screen, FakeNavigator(screen), RecipesProducer(repository))

    presenter.test {
      assertIs<RecipesState.Loading>(awaitItem())

      repository.responses.value =
        StoreReadResponse.Data(listOf(recipe("1")), StoreReadResponseOrigin.Fetcher())
      assertIs<RecipesState.Success>(awaitItem())

      repository.responses.value =
        StoreReadResponse.Data(
          listOf(recipe("1"), recipe("2")),
          StoreReadResponseOrigin.SourceOfTruth,
        )
      val state = assertIs<RecipesState.Success>(awaitItem())
      assertEquals(2, state.recipes.size)
      // The UI names the list from this; `Screen.title()` lives in `:ui` and cannot be reached
      // here.
      assertEquals(screen, state.screen)

      assertEquals(1, repository.ingredientSubscriptions)
      assertEquals(setOf("chicken", "rice"), repository.lastIngredients)
    }
  }

  // Regression: a search that matches nothing is an answer, not a pending request. It used to be
  // unreachable -- the repository's Store validator called an empty result invalid, so the store
  // refetched instead of emitting it and the screen sat on a spinner while the network was
  // hammered.
  test("a search with no matches settles on an empty success") {
    val screen = RecipesScreen.BySearch("zzzzqqq")
    val repository = FakeRecipeRepository()
    val presenter = RecipesPresenter(screen, FakeNavigator(screen), RecipesProducer(repository))

    presenter.test {
      assertIs<RecipesState.Loading>(awaitItem())

      repository.responses.value =
        StoreReadResponse.Data(emptyList(), StoreReadResponseOrigin.SourceOfTruth)

      val state = assertIs<RecipesState.Success>(awaitItem())
      assertEquals(emptyList(), state.recipes)
      assertEquals(screen, state.screen)
    }
  }
}
