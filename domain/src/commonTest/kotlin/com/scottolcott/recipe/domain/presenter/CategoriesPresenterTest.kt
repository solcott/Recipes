package com.scottolcott.recipe.domain.presenter

import com.scottolcott.recipe.domain.producer.CategoriesProducer
import com.scottolcott.recipe.model.Category
import com.scottolcott.recipe.model.CategoryId
import com.scottolcott.recipe.repository.CategoryRepository
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

private class CategoriesTestEnvironment(
  val navigator: FakeNavigator,
  val repository: FakeCategoryRepository,
  val presenter: CategoriesPresenter,
)

private class FakeCategoryRepository : CategoryRepository {
  var getCategoriesHandler: () -> Flow<Outcome<List<Category>>> = { inFlight() }

  var getCategoriesByNameHandler: (String) -> Flow<Outcome<List<Category>>> = {
    getCategoriesHandler()
  }

  override fun getCategories(): Flow<Outcome<List<Category>>> = getCategoriesHandler()

  override fun getCategories(nameFilter: String): Flow<Outcome<List<Category>>> =
    getCategoriesByNameHandler(nameFilter)
}

val categoriesPresenterTests by testSuite {
  val categoriesFixture = testFixture {
    listOf(Category(CategoryId("1"), "Category 1", "thumb1", "desc1", Clock.System.now()))
  }

  val environmentFixture = testFixture {
    val navigator = FakeNavigator(HomeScreen())
    val repository = FakeCategoryRepository()
    val presenter = CategoriesPresenter(navigator, CategoriesProducer(repository))
    CategoriesTestEnvironment(navigator, repository, presenter)
  }

  environmentFixture asContextForEach
    {
      val simpleStates =
        listOf(
          Triple("loadingState", inFlight()) { state: CategoriesState ->
            assertIs<CategoriesState.Loading>(state)
          },
          Triple("successState", null) { state: CategoriesState ->
            assertIs<CategoriesState.Success>(state)
          },
          Triple(
            "errorState",
            flowOf(Outcome.Error(DataError.Unknown(message = "Error"), Origin.Network)),
          ) { state: CategoriesState ->
            assertIs<CategoriesState.Error>(state)
          },
        )

      for ((name, responseFlow, assertion) in simpleStates) {
        test(name) {
          val flow = responseFlow ?: flowOf(Outcome.Data(categoriesFixture(), Origin.Cache))
          repository.getCategoriesHandler = { flow }
          presenter.test {
            var state = awaitItem()
            if (state is CategoriesState.Loading && name != "loadingState") {
              state = awaitItem()
            }
            assertion(state)
            if (state is CategoriesState.Success) {
              assertEquals(categoriesFixture(), state.categories)
            }
          }
        }
      }

      test("retryClick") {
        var callCount = 0
        repository.getCategoriesHandler = {
          callCount++
          if (callCount == 1) {
            flowOf(Outcome.Error(DataError.Unknown(message = "Error"), Origin.Network))
          } else {
            inFlight()
          }
        }
        presenter.test {
          var state = awaitItem()
          if (state is CategoriesState.Loading) state = awaitItem()
          assertIs<CategoriesState.Error>(state)

          state.eventSink(CategoriesEvent.Error.RetryClicked)

          val nextState = awaitItem()
          assertIs<CategoriesState.Loading>(nextState)
          assertEquals(2, callCount)
        }
      }

      test("categoryClick") {
        val categories = categoriesFixture()
        repository.getCategoriesHandler = { flowOf(Outcome.Data(categories, Origin.Cache)) }
        presenter.test {
          var state = awaitItem()
          if (state is CategoriesState.Loading) state = awaitItem()
          assertIs<CategoriesState.Success>(state)

          state.eventSink(CategoriesEvent.Success.CategoryClicked("Category 1"))

          val lastScreen = navigator.awaitNextScreen()
          assertIs<RecipesScreen.ByCategory>(lastScreen)
          assertEquals("Category 1", lastScreen.category)
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
