package com.scottolcott.recipe.domain.presenter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.scottolcott.recipe.domain.producer.RecipesProducer
import com.scottolcott.recipe.model.Recipe
import com.scottolcott.recipe.model.RecipeId
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.CircuitUiEvent
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
class RecipesPresenter(
  @Assisted private val screen: Screen,
  @Assisted private val navigator: Navigator,
  private val recipesProducer: RecipesProducer,
) : Presenter<RecipesState> {
  @Composable
  override fun present(): RecipesState {
    var retryTrigger by rememberRetained { mutableIntStateOf(0) }
    val showAreaLabel = screen is RecipesScreen.BySearch
    screen as RecipesScreen
    val recipesResponse =
      when (screen) {
        is RecipesScreen.ByCategory ->
          recipesProducer.produceByCategory(screen.category, retryTrigger)
        is RecipesScreen.BySearch ->
          recipesProducer.produceBySearchTerm(screen.searchTerm, retryTrigger)
        is RecipesScreen.Favorites -> recipesProducer.produceByFavorites(retryTrigger)
      }
    var lastRecipes by rememberRetained(retryTrigger) { mutableStateOf<List<Recipe>?>(null) }
    if (recipesResponse is StoreReadResponse.Data) {
      lastRecipes = recipesResponse.value
    }
    val errorEventSink: (RecipesEvent.Error) -> Unit = { event ->
      when (event) {
        RecipesEvent.Error.RetryClicked -> retryTrigger++
      }
    }
    val successEventSink: (RecipesEvent.Success) -> Unit = { event ->
      when (event) {
        is RecipesEvent.Success.RecipeClicked -> navigator.goTo(RecipeDetailsScreen(event.id))
      }
    }
    return when (recipesResponse) {
      is StoreReadResponse.Data<List<Recipe>> ->
        RecipesState.Success(
          recipesResponse.value,
          isRefreshing = false,
          showAreaLabel = showAreaLabel,
          successEventSink,
        )

      is StoreReadResponse.Error.Exception ->
        RecipesState.Error(recipesResponse.error.message ?: "Unknown Error", errorEventSink)

      is StoreReadResponse.Error.Message ->
        RecipesState.Error(recipesResponse.message, errorEventSink)
      // TODO not sure how to handle this
      is StoreReadResponse.Error.Custom<*> ->
        RecipesState.Error(recipesResponse.toString(), errorEventSink)

      is StoreReadResponse.Initial,
      is StoreReadResponse.Loading,
      is StoreReadResponse.NoNewData -> {
        val cached = lastRecipes
        if (cached != null) {
          RecipesState.Success(
            cached,
            isRefreshing = true,
            showAreaLabel = showAreaLabel,
            successEventSink,
          )
        } else {
          RecipesState.Loading
        }
      }
    }
  }

  @CircuitInject(RecipesScreen::class, AppScope::class)
  @AssistedFactory
  interface Factory {
    @Suppress("unused") fun create(screen: Screen, navigator: Navigator): RecipesPresenter
  }
}

sealed interface RecipesEvent : CircuitUiEvent {
  sealed interface Error : RecipesEvent {
    data object RetryClicked : Error
  }

  sealed interface Success : RecipesEvent {
    data class RecipeClicked(val id: RecipeId) : Success
  }
}

sealed interface RecipesState : CircuitUiState {
  data object Loading : RecipesState

  data class Error(val message: String, val eventSink: (RecipesEvent.Error) -> Unit) : RecipesState

  data class Success(
    val recipes: List<Recipe>,
    val isRefreshing: Boolean,
    val showAreaLabel: Boolean,
    @Redacted val eventSink: (RecipesEvent.Success) -> Unit,
  ) : RecipesState
}

@Parcelize
sealed interface RecipesScreen : Screen {
  @Parcelize data class ByCategory(val category: String) : RecipesScreen

  @Parcelize data class BySearch(val searchTerm: String) : RecipesScreen

  @Parcelize data object Favorites : RecipesScreen
}
