package com.scottolcott.recipe.domain.presenter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import com.scottolcott.recipe.domain.producer.AreasProducer
import com.scottolcott.recipe.model.Area
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.serialization.CircuitSerializable
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.redacted.annotations.Redacted
import org.mobilenativefoundation.store.store5.StoreReadResponse

@CircuitInject(AreasScreen::class, AppScope::class)
@Inject
class AreasPresenter
internal constructor(
  private val navigator: Navigator,
  private val areasProducer: AreasProducer,
) : Presenter<AreasState> {
  @Composable
  override fun present(): AreasState {
    var retryTrigger by retain { mutableIntStateOf(0) }
    val response = areasProducer.produce(retryTrigger)
    var lastAreas by retain(retryTrigger) { mutableStateOf<List<Area>?>(null) }

    if (response is StoreReadResponse.Data) {
      lastAreas = response.value
    }

    val successEventSink: (AreasEvent.Success) -> Unit = remember {
      { event ->
        when (event) {
          is AreasEvent.Success.AreaClicked -> {
            navigator.goTo(RecipesScreen.ByArea(event.area))
          }
        }
      }
    }

    val errorEventSink: (AreasEvent.Error) -> Unit = remember {
      { event ->
        when (event) {
          AreasEvent.Error.RetryClicked -> {
            retryTrigger++
          }
        }
      }
    }
    return when (response) {
      is StoreReadResponse.Initial,
      is StoreReadResponse.Loading,
      is StoreReadResponse.NoNewData -> {
        val cached = lastAreas
        if (cached != null) {
          AreasState.Success(
            areas = cached,
            isRefreshing = true,
            eventSink = successEventSink,
          )
        } else {
          AreasState.Loading
        }
      }

      is StoreReadResponse.Data ->
        AreasState.Success(
          areas = response.value,
          isRefreshing = false,
          eventSink = successEventSink,
        )

      is StoreReadResponse.Error.Exception ->
        AreasState.Error(
          message = response.error.message ?: "Unknown error",
          eventSink = errorEventSink,
        )

      is StoreReadResponse.Error.Message ->
        AreasState.Error(message = response.message, eventSink = errorEventSink)
      // TODO not sure what to do here
      is StoreReadResponse.Error.Custom<*> ->
        AreasState.Error(message = response.toString(), eventSink = errorEventSink)
    }
  }
}

sealed interface AreasState : CircuitUiState {

  data object Loading : AreasState

  data class Error(val message: String, @Redacted val eventSink: (AreasEvent.Error) -> Unit) :
    AreasState

  data class Success(
    val areas: List<Area>,
    val isRefreshing: Boolean,
    @Redacted val eventSink: (AreasEvent.Success) -> Unit,
  ) : AreasState
}

sealed interface AreasEvent {
  sealed interface Success : AreasEvent {
    data class AreaClicked(val area: String) : Success
  }

  sealed interface Error : AreasEvent {
    data object RetryClicked : Error
  }
}

@CircuitSerializable(AppScope::class)
data object AreasScreen : HomeTabScreen {
  override val urlSegment: String = "areas"
}
