package com.scottolcott.recipe.domain.presenter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import com.scottolcott.recipe.domain.producer.RecipesProducer
import com.scottolcott.recipe.model.Recipe
import com.scottolcott.recipe.model.RecipeId
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.serialization.CircuitSerializable
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.redacted.annotations.Redacted
import io.github.solcott.uistate.ContentState

@CircuitInject(RecipesScreen::class, AppScope::class)
@Inject
class RecipesPresenter
internal constructor(
  private val screen: RecipesScreen,
  private val navigator: Navigator,
  private val recipesProducer: RecipesProducer,
) : Presenter<RecipesState> {
  @Composable
  override fun present(): RecipesState {
    var retryTrigger by retain { mutableIntStateOf(0) }
    val showAreaLabel = screen is RecipesScreen.BySearch
    val state = produceRecipesState(screen, retryTrigger)

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

    return state.foldToState(
      onLoading = { RecipesState.Loading },
      onError = { message -> RecipesState.Error(message, errorEventSink) },
      onContent = { recipes, isRefreshing ->
        RecipesState.Success(
          screen,
          recipes,
          isRefreshing = isRefreshing,
          showAreaLabel = showAreaLabel,
          successEventSink,
        )
      },
    )
  }

  @Composable
  private fun produceRecipesState(
    screen: RecipesScreen,
    retryTrigger: Int,
  ): ContentState<List<Recipe>> {
    return when (screen) {
      is RecipesScreen.ByCategory ->
        recipesProducer.produceByCategory(screen.category, retryTrigger)
      is RecipesScreen.BySearch ->
        recipesProducer.produceBySearchTerm(screen.searchTerm, retryTrigger)
      is RecipesScreen.Favorites -> recipesProducer.produceByFavorites(retryTrigger)
      is RecipesScreen.ByArea -> recipesProducer.produceByArea(screen.area, retryTrigger)
      is RecipesScreen.ByIngredient ->
        recipesProducer.produceByIngredients(screen.ingredients, retryTrigger)
    }
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
    /**
     * The screen this list was built for, so the UI can name it.
     *
     * `Screen.title()` lives in `:ui` because it resolves compose resources, so the screen has to
     * travel to reach it -- the name cannot be resolved here.
     */
    val screen: RecipesScreen,
    val recipes: List<Recipe>,
    val isRefreshing: Boolean,
    val showAreaLabel: Boolean,
    @Redacted val eventSink: (RecipesEvent.Success) -> Unit,
  ) : RecipesState
}

sealed interface RecipesScreen : Screen {
  @CircuitSerializable(AppScope::class) data class ByCategory(val category: String) : RecipesScreen

  @CircuitSerializable(AppScope::class) data class ByArea(val area: String) : RecipesScreen

  @CircuitSerializable(AppScope::class) data class BySearch(val searchTerm: String) : RecipesScreen

  @CircuitSerializable(AppScope::class) data object Favorites : RecipesScreen

  /**
   * Recipes containing *every* one of [ingredients]. A set, not a single name: the filter ANDs the
   * whole collection, and set equality is order-independent so two routes naming the same
   * ingredients are the same screen.
   */
  @CircuitSerializable(AppScope::class)
  data class ByIngredient(val ingredients: Set<String>) : RecipesScreen
}
