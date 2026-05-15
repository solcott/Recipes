package com.scottolcott.recipe.domain.presenter

import com.scottolcott.recipe.domain.producer.CategoriesProducer
import com.scottolcott.recipe.model.Category
import com.scottolcott.recipe.model.CategoryId
import com.scottolcott.recipe.repository.CategoryRepository
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

private class CategoriesTestEnvironment(
  val navigator: FakeNavigator,
  val repository: FakeCategoryRepository,
  val presenter: CategoriesPresenter,
)

private class FakeCategoryRepository : CategoryRepository {
  var getCategoriesHandler: () -> Flow<StoreReadResponse<List<Category>>> = {
    flowOf(StoreReadResponse.Initial)
  }

  override fun getCategories(): Flow<StoreReadResponse<List<Category>>> = getCategoriesHandler()
}

val categoriesPresenterTests by testSuite {
  val categoriesFixture = testFixture {
    listOf(Category(CategoryId("1"), "Category 1", "thumb1", "desc1", Clock.System.now()))
  }

  val environmentFixture = testFixture {
    val navigator = FakeNavigator(CategoriesScreen)
    val repository = FakeCategoryRepository()
    val presenter = CategoriesPresenter(navigator, CategoriesProducer(repository))
    CategoriesTestEnvironment(navigator, repository, presenter)
  }

  environmentFixture asContextForEach
    {
      val simpleStates =
        listOf(
          Triple("loadingState", flowOf(StoreReadResponse.Initial)) { state: CategoriesState ->
            assertIs<CategoriesState.Loading>(state)
          },
          Triple("successState", null) { state: CategoriesState ->
            assertIs<CategoriesState.Success>(state)
          },
          Triple(
            "errorState",
            flowOf(
              StoreReadResponse.Error.Exception(
                RuntimeException("Error"),
                StoreReadResponseOrigin.Fetcher(),
              )
            ),
          ) { state: CategoriesState ->
            assertIs<CategoriesState.Error>(state)
          },
        )

      for ((name, responseFlow, assertion) in simpleStates) {
        test(name) {
          val flow =
            responseFlow
              ?: flowOf(
                StoreReadResponse.Data(categoriesFixture(), StoreReadResponseOrigin.SourceOfTruth)
              )
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
        repository.getCategoriesHandler = {
          flowOf(StoreReadResponse.Data(categories, StoreReadResponseOrigin.SourceOfTruth))
        }
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
