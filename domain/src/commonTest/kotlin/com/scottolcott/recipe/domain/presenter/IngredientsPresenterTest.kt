package com.scottolcott.recipe.domain.presenter

import com.scottolcott.recipe.domain.producer.IngredientsProducer
import com.scottolcott.recipe.model.Ingredient
import com.scottolcott.recipe.model.IngredientId
import com.scottolcott.recipe.repository.IngredientRepository
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.mobilenativefoundation.store.store5.StoreReadResponse
import org.mobilenativefoundation.store.store5.StoreReadResponseOrigin

private class IngredientsTestEnvironment(
  val navigator: FakeNavigator,
  val repository: FakeIngredientRepository,
  val presenter: IngredientsPresenter,
)

private class FakeIngredientRepository : IngredientRepository {
  var getIngredientsHandler: () -> Flow<StoreReadResponse<List<Ingredient>>> = {
    flowOf(StoreReadResponse.Initial)
  }

  override fun getIngredients(): Flow<StoreReadResponse<List<Ingredient>>> = getIngredientsHandler()

  override fun filterIngredientsByName(
    nameFilter: String
  ): Flow<StoreReadResponse<List<Ingredient>>> = getIngredientsHandler()
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
          Triple("loadingState", flowOf(StoreReadResponse.Initial)) { state: IngredientsState ->
            assertIs<IngredientsState.Loading>(state)
          },
          Triple("successState", null) { state: IngredientsState ->
            assertIs<IngredientsState.Success>(state)
          },
          Triple(
            "errorState",
            flowOf(
              StoreReadResponse.Error.Exception(
                RuntimeException("Error"),
                StoreReadResponseOrigin.Fetcher(),
              )
            ),
          ) { state: IngredientsState ->
            assertIs<IngredientsState.Error>(state)
          },
        )

      for ((name, responseFlow, assertion) in simpleStates) {
        test(name) {
          val flow =
            responseFlow
              ?: flowOf(
                StoreReadResponse.Data(ingredientsFixture(), StoreReadResponseOrigin.SourceOfTruth)
              )
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
            flowOf(
              StoreReadResponse.Error.Exception(
                RuntimeException("Error"),
                StoreReadResponseOrigin.Fetcher(),
              )
            )
          } else {
            flowOf(StoreReadResponse.Initial)
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
        repository.getIngredientsHandler = {
          flowOf(StoreReadResponse.Data(ingredients, StoreReadResponseOrigin.SourceOfTruth))
        }
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
