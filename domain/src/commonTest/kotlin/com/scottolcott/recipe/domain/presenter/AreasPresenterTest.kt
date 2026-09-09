package com.scottolcott.recipe.domain.presenter

import com.scottolcott.recipe.domain.producer.AreasProducer
import com.scottolcott.recipe.model.Area
import com.scottolcott.recipe.repository.AreaRepository
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import org.mobilenativefoundation.store.store5.StoreReadResponse
import org.mobilenativefoundation.store.store5.StoreReadResponseOrigin

private class AreasTestEnvironment(
  val navigator: FakeNavigator,
  val repository: FakeAreaRepository,
  val presenter: AreasPresenter,
)

private class FakeAreaRepository : AreaRepository {
  var getAreasHandler: () -> Flow<StoreReadResponse<List<Area>>> = {
    flowOf(StoreReadResponse.Initial)
  }

  override fun getAreas(): Flow<StoreReadResponse<List<Area>>> = getAreasHandler()

  override suspend fun countryFor(area: String): String? = null
}

val areasPresenterTests by testSuite {
  val areasFixture = testFixture {
    listOf(Area("Italian", "Italy", Clock.System.now()), Area("Unknown", null, Clock.System.now()))
  }

  val environmentFixture = testFixture {
    val navigator = FakeNavigator(HomeScreen())
    val repository = FakeAreaRepository()
    val presenter = AreasPresenter(navigator, AreasProducer(repository))
    AreasTestEnvironment(navigator, repository, presenter)
  }

  environmentFixture asContextForEach
    {
      val simpleStates =
        listOf(
          Triple("loadingState", flowOf(StoreReadResponse.Initial)) { state: AreasState ->
            assertIs<AreasState.Loading>(state)
          },
          Triple("successState", null) { state: AreasState -> assertIs<AreasState.Success>(state) },
          Triple(
            "errorState",
            flowOf(
              StoreReadResponse.Error.Exception(
                RuntimeException("Error"),
                StoreReadResponseOrigin.Fetcher(),
              )
            ),
          ) { state: AreasState ->
            assertIs<AreasState.Error>(state)
          },
        )

      for ((name, responseFlow, assertion) in simpleStates) {
        test(name) {
          val flow =
            responseFlow
              ?: flowOf(
                StoreReadResponse.Data(areasFixture(), StoreReadResponseOrigin.SourceOfTruth)
              )
          repository.getAreasHandler = { flow }
          presenter.test {
            var state = awaitItem()
            if (state is AreasState.Loading && name != "loadingState") {
              state = awaitItem()
            }
            assertion(state)
            if (state is AreasState.Success) {
              assertEquals(areasFixture(), state.areas)
            }
          }
        }
      }

      // The last-known list survives a return to Loading, so a background refresh renders as
      // isRefreshing over the stale grid rather than dropping back to a spinner.
      test("refreshKeepsPreviousAreas") {
        val areas = areasFixture()
        // Driven emission by emission: a flow that emits both up front collapses into a single
        // recomposition, which would hide the transition this test is about.
        val responses = MutableSharedFlow<StoreReadResponse<List<Area>>>(replay = 1)
        responses.emit(StoreReadResponse.Data(areas, StoreReadResponseOrigin.SourceOfTruth))
        repository.getAreasHandler = { responses }

        presenter.test {
          var state = awaitItem()
          if (state is AreasState.Loading) state = awaitItem()
          assertIs<AreasState.Success>(state)
          assertEquals(false, state.isRefreshing)

          responses.emit(StoreReadResponse.Loading(StoreReadResponseOrigin.Fetcher()))

          val refreshing = awaitItem()
          assertIs<AreasState.Success>(refreshing)
          assertEquals(true, refreshing.isRefreshing)
          assertEquals(areas, refreshing.areas)
        }
      }

      test("retryClick") {
        var callCount = 0
        repository.getAreasHandler = {
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
          if (state is AreasState.Loading) state = awaitItem()
          assertIs<AreasState.Error>(state)

          state.eventSink(AreasEvent.Error.RetryClicked)

          val nextState = awaitItem()
          assertIs<AreasState.Loading>(nextState)
          assertEquals(2, callCount)
        }
      }

      test("areaClick") {
        val areas = areasFixture()
        repository.getAreasHandler = {
          flowOf(StoreReadResponse.Data(areas, StoreReadResponseOrigin.SourceOfTruth))
        }
        presenter.test {
          var state = awaitItem()
          if (state is AreasState.Loading) state = awaitItem()
          assertIs<AreasState.Success>(state)

          state.eventSink(AreasEvent.Success.AreaClicked("Italian"))

          val lastScreen = navigator.awaitNextScreen()
          assertIs<RecipesScreen.ByArea>(lastScreen)
          assertEquals("Italian", lastScreen.area)
        }
      }
    }
}
