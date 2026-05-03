package com.scottolcott.recipe.domain.presenter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import co.touchlab.kermit.Logger
import com.scottolcott.recipe.domain.presenter.CategoriesState.*
import com.scottolcott.recipe.domain.producer.CategoriesProducer
import com.scottolcott.recipe.model.Category
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.redacted.annotations.Redacted
import io.github.solcott.kmp.parcelize.Parcelize
import org.mobilenativefoundation.store.store5.StoreReadResponse

@AssistedInject
class CategoriesPresenter(
  @Assisted private val navigator: Navigator,
  private val categoriesProducer: CategoriesProducer,
  private val logger: Logger,
) : Presenter<CategoriesState> {
  @Composable
  override fun present(): CategoriesState {
    var retryTrigger by rememberRetained { mutableIntStateOf(0) }
    val response = categoriesProducer.produce(retryTrigger)
    var lastCategories by rememberRetained(retryTrigger) { mutableStateOf<List<Category>?>(null) }

    if (response is StoreReadResponse.Data) {
      lastCategories = response.value
    }

    val successEventSink: (CategoriesEvent.Success) -> Unit = remember {
      { event ->
        when (event) {
          is CategoriesEvent.Success.CategoryClicked -> {
            navigator.goTo(RecipesScreen.ByCategory(event.category))
          }
        }
      }
    }

    val errorEventSink: (CategoriesEvent.Error) -> Unit = remember {
      { event ->
        when (event) {
          CategoriesEvent.Error.RetryClicked -> {
            retryTrigger++
          }
        }
      }
    }

    return when (response) {
      is StoreReadResponse.Initial,
      is StoreReadResponse.Loading,
      is StoreReadResponse.NoNewData -> {
        val cached = lastCategories
        if (cached != null) {
          Success(categories = cached, isRefreshing = true, eventSink = successEventSink)
        } else {
          Loading
        }
      }

      is StoreReadResponse.Data ->
        Success(categories = response.value, isRefreshing = false, eventSink = successEventSink)

      is StoreReadResponse.Error.Exception ->
        Error(message = response.error.message ?: "Unknown error", eventSink = errorEventSink)

      is StoreReadResponse.Error.Message ->
        Error(message = response.message, eventSink = errorEventSink)
      // TODO not sure what to do here
      is StoreReadResponse.Error.Custom<*> ->
        Error(message = response.toString(), eventSink = errorEventSink)
    }
  }

  @CircuitInject(CategoriesScreen::class, AppScope::class)
  @AssistedFactory
  interface Factory {
    @Suppress("unused") fun create(navigator: Navigator): CategoriesPresenter
  }
}

sealed class CategoriesState : CircuitUiState {
  data object Loading : CategoriesState()

  data class Error(val message: String, @Redacted val eventSink: (CategoriesEvent.Error) -> Unit) :
    CategoriesState()

  data class Success(
    val categories: List<Category>,
    val isRefreshing: Boolean,
    @Redacted val eventSink: (CategoriesEvent.Success) -> Unit,
  ) : CategoriesState()
}

sealed class CategoriesEvent {
  sealed class Success : CategoriesEvent() {
    data class CategoryClicked(val category: String) : Success()
  }

  sealed class Error : CategoriesEvent() {
    data object RetryClicked : Error()
  }
}

@Parcelize data object CategoriesScreen : Screen
