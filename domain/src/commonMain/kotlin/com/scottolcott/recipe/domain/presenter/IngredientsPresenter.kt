package com.scottolcott.recipe.domain.presenter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import com.scottolcott.recipe.domain.producer.IngredientsProducer
import com.scottolcott.recipe.model.Ingredient
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.serialization.CircuitSerializable
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.redacted.annotations.Redacted
import org.mobilenativefoundation.store.store5.StoreReadResponse

@CircuitInject(IngredientsScreen::class, AppScope::class)
@Inject
class IngredientsPresenter
internal constructor(
  private val navigator: Navigator,
  private val ingredientsProducer: IngredientsProducer,
) : Presenter<IngredientsState> {
  @Composable
  override fun present(): IngredientsState {
    var retryTrigger by retain { mutableIntStateOf(0) }
    val response = ingredientsProducer.produce(retryTrigger)
    var lastIngredients by retain(retryTrigger) { mutableStateOf<List<Ingredient>?>(null) }

    if (response is StoreReadResponse.Data) {
      lastIngredients = response.value
    }

    val successEventSink: (IngredientsEvent.Success) -> Unit = remember {
      { event ->
        when (event) {
          is IngredientsEvent.Success.IngredientClicked -> {
            navigator.goTo(RecipesScreen.ByIngredient(setOf(event.ingredient)))
          }
        }
      }
    }

    val errorEventSink: (IngredientsEvent.Error) -> Unit = remember {
      { event ->
        when (event) {
          IngredientsEvent.Error.RetryClicked -> {
            retryTrigger++
          }
        }
      }
    }
    return when (response) {
      is StoreReadResponse.Initial,
      is StoreReadResponse.Loading,
      is StoreReadResponse.NoNewData -> {
        val cached = lastIngredients
        if (cached != null) {
          IngredientsState.Success(
            ingredients = cached,
            isRefreshing = true,
            eventSink = successEventSink,
          )
        } else {
          IngredientsState.Loading
        }
      }

      is StoreReadResponse.Data ->
        IngredientsState.Success(
          ingredients = response.value,
          isRefreshing = false,
          eventSink = successEventSink,
        )

      is StoreReadResponse.Error.Exception ->
        IngredientsState.Error(
          message = response.error.message ?: "Unknown error",
          eventSink = errorEventSink,
        )

      is StoreReadResponse.Error.Message ->
        IngredientsState.Error(message = response.message, eventSink = errorEventSink)
      // TODO not sure what to do here
      is StoreReadResponse.Error.Custom<*> ->
        IngredientsState.Error(message = response.toString(), eventSink = errorEventSink)
    }
  }
}

sealed interface IngredientsState : CircuitUiState {

  data object Loading : IngredientsState

  data class Error(val message: String, @Redacted val eventSink: (IngredientsEvent.Error) -> Unit) :
    IngredientsState

  data class Success(
    val ingredients: List<Ingredient>,
    val isRefreshing: Boolean,
    @Redacted val eventSink: (IngredientsEvent.Success) -> Unit,
  ) : IngredientsState
}

sealed interface IngredientsEvent {
  sealed interface Success : IngredientsEvent {
    data class IngredientClicked(val ingredient: String) : Success
  }

  sealed interface Error : IngredientsEvent {
    data object RetryClicked : Error
  }
}

@CircuitSerializable(AppScope::class)
data object IngredientsScreen : HomeTabScreen {
  override val urlSegment: String = "ingredients"
}
