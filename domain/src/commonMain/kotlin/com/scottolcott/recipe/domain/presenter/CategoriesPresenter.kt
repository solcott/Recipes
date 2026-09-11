package com.scottolcott.recipe.domain.presenter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import com.scottolcott.recipe.domain.presenter.CategoriesState.Error
import com.scottolcott.recipe.domain.presenter.CategoriesState.Loading
import com.scottolcott.recipe.domain.presenter.CategoriesState.Success
import com.scottolcott.recipe.domain.producer.CategoriesProducer
import com.scottolcott.recipe.model.Category
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.serialization.CircuitSerializable
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.redacted.annotations.Redacted

@CircuitInject(CategoriesScreen::class, AppScope::class)
@Inject
class CategoriesPresenter
internal constructor(
  private val navigator: Navigator,
  private val categoriesProducer: CategoriesProducer,
) : Presenter<CategoriesState> {
  @Composable
  override fun present(): CategoriesState {
    var retryTrigger by retain { mutableIntStateOf(0) }
    val state = categoriesProducer.produce(retryTrigger)

    val successEventSink: (CategoriesEvent.Success) -> Unit = remember {
      { event ->
        when (event) {
          is CategoriesEvent.Success.CategoryClicked ->
            navigator.goTo(RecipesScreen.ByCategory(event.category))
        }
      }
    }

    val errorEventSink: (CategoriesEvent.Error) -> Unit = remember {
      { event ->
        when (event) {
          CategoriesEvent.Error.RetryClicked -> retryTrigger++
        }
      }
    }

    return state.foldToState(
      onLoading = { Loading },
      onError = { message -> Error(message, errorEventSink) },
      onContent = { items, isRefreshing -> Success(items, isRefreshing, successEventSink) },
    )
  }
}

sealed interface CategoriesState : CircuitUiState {
  data object Loading : CategoriesState

  data class Error(val message: String, @Redacted val eventSink: (CategoriesEvent.Error) -> Unit) :
    CategoriesState

  data class Success(
    val categories: List<Category>,
    val isRefreshing: Boolean,
    @Redacted val eventSink: (CategoriesEvent.Success) -> Unit,
  ) : CategoriesState
}

sealed interface CategoriesEvent {
  sealed interface Success : CategoriesEvent {
    data class CategoryClicked(val category: String) : Success
  }

  sealed interface Error : CategoriesEvent {
    data object RetryClicked : Error
  }
}

@CircuitSerializable(AppScope::class)
data object CategoriesScreen : HomeTabScreen {
  override val urlSegment: String = "categories"
}
