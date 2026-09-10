package com.scottolcott.recipe.domain.presenter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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

    val successEventSink: (IngredientsEvent.Success) -> Unit = remember {
      { event ->
        when (event) {
          is IngredientsEvent.Success.IngredientClicked ->
            navigator.goTo(RecipesScreen.ByIngredient(setOf(event.ingredient)))
        }
      }
    }

    val errorEventSink: (IngredientsEvent.Error) -> Unit = remember {
      { event ->
        when (event) {
          IngredientsEvent.Error.RetryClicked -> retryTrigger++
        }
      }
    }

    return when (val ui = rememberListUi(response, retryTrigger)) {
      ListUi.Loading -> IngredientsState.Loading
      is ListUi.Content -> IngredientsState.Success(ui.items, ui.isRefreshing, successEventSink)
      is ListUi.Failure -> IngredientsState.Error(ui.message, errorEventSink)
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
