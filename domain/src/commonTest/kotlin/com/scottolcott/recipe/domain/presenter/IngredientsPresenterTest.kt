package com.scottolcott.recipe.domain.presenter

import com.scottolcott.recipe.domain.producer.IngredientsProducer
import com.scottolcott.recipe.model.Ingredient
import com.scottolcott.recipe.model.IngredientId
import com.scottolcott.recipe.repository.IngredientRepository
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import de.infix.testBalloon.framework.core.testSuite
import io.github.solcott.dataresult.DataError
import io.github.solcott.dataresult.Origin
import io.github.solcott.dataresult.Outcome
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Clock
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

private class IngredientsTestEnvironment(
  val navigator: FakeNavigator,
  val repository: FakeIngredientRepository,
  val presenter: IngredientsPresenter,
)

private class FakeIngredientRepository : IngredientRepository {
  var getIngredientsHandler: () -> Flow<Outcome<List<Ingredient>>> = { inFlight() }

  override fun getIngredients(): Flow<Outcome<List<Ingredient>>> = getIngredientsHandler()

  override fun filterIngredientsByName(nameFilter: String): Flow<Outcome<List<Ingredient>>> =
    getIngredientsHandler()
}

val ingredientsPresenterTests by testSuite {
  val ingredientsFixture = testFixture {
    listOf(Ingredient(IngredientId("1"), "Chicken", lastFetched = Clock.System.now()))
  }

  val environmentFixture = testFixture {
    val navigator = FakeNavigator(HomeScreen())
    val repository = FakeIngredientRepository()
    val presenter = IngredientsPresenter(navigator, IngredientsProducer(repository))
    IngredientsTestEnvironment(navigator, repository, presenter)
  }

  environmentFixture asContextForEach
    {
      val simpleStates =
        listOf(
          Triple("loadingState", inFlight()) { state: IngredientsState ->
            assertIs<IngredientsState.Loading>(state)
          },
          Triple("successState", null) { state: IngredientsState ->
            assertIs<IngredientsState.Success>(state)
          },
          Triple(
            "errorState",
            flowOf(Outcome.Error(DataError.Unknown(message = "Error"), Origin.Network)),
          ) { state: IngredientsState ->
            assertIs<IngredientsState.Error>(state)
          },
        )

      for ((name, responseFlow, assertion) in simpleStates) {
        test(name) {
          val flow = responseFlow ?: flowOf(Outcome.Data(ingredientsFixture(), Origin.Cache))
          repository.getIngredientsHandler = { flow }
          presenter.test {
            var state = awaitItem()
            if (state is IngredientsState.Loading && name != "loadingState") {
              state = awaitItem()
            }
            assertion(state)
            if (state is IngredientsState.Success) {
              assertEquals(ingredientsFixture(), state.ingredients)
            }
          }
        }
      }

      test("retryClick") {
        var callCount = 0
        repository.getIngredientsHandler = {
          callCount++
          if (callCount == 1) {
            flowOf(Outcome.Error(DataError.Unknown(message = "Error"), Origin.Network))
          } else {
            inFlight()
          }
        }
        presenter.test {
          var state = awaitItem()
          if (state is IngredientsState.Loading) state = awaitItem()
          assertIs<IngredientsState.Error>(state)

          state.eventSink(IngredientsEvent.Error.RetryClicked)

          val nextState = awaitItem()
          assertIs<IngredientsState.Loading>(nextState)
          assertEquals(2, callCount)
        }
      }

      test("ingredientClick") {
        val ingredients = ingredientsFixture()
        repository.getIngredientsHandler = { flowOf(Outcome.Data(ingredients, Origin.Cache)) }
        presenter.test {
          var state = awaitItem()
          if (state is IngredientsState.Loading) state = awaitItem()
          assertIs<IngredientsState.Success>(state)

          state.eventSink(IngredientsEvent.Success.IngredientClicked("Chicken"))

          val lastScreen = navigator.awaitNextScreen()
          assertIs<RecipesScreen.ByIngredient>(lastScreen)
          assertEquals(setOf("Chicken"), lastScreen.ingredients)
        }
      }
    }
}

/**
 * A request that is still in flight: it reports loading and then stays open.
 *
 * Deliberately not `flowOf(Outcome.Loading)`. That completes, and a completed source that produced
 * nothing is a *settled* empty result, not a pending one -- `produceRetainedContentState` settles
 * the status on completion so a spinner can never hang. A Store stream never completes, so this is
 * the faithful stand-in.
 */
private fun inFlight(): Flow<Outcome<Nothing>> = flow {
  emit(Outcome.Loading)
  awaitCancellation()
}
