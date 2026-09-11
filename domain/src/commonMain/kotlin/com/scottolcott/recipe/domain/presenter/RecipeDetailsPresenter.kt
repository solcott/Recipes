package com.scottolcott.recipe.domain.presenter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.scottolcott.recipe.domain.presenter.RecipesScreen.ByArea
import com.scottolcott.recipe.domain.presenter.RecipesScreen.ByCategory
import com.scottolcott.recipe.model.Recipe
import com.scottolcott.recipe.model.RecipeId
import com.scottolcott.recipe.repository.RecipeRepository
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
import io.github.solcott.uistate.circuit.produceRetainedContentState
import io.github.solcott.uistate.errorOrNull
import io.github.solcott.uistate.isLoading
import kotlinx.coroutines.launch

@CircuitInject(RecipeDetailsScreen::class, AppScope::class)
@Inject
class RecipeDetailsPresenter
internal constructor(
  private val screen: RecipeDetailsScreen,
  private val navigator: Navigator,
  private val recipeRepository: RecipeRepository,
) : Presenter<RecipeDetailsState> {
  @Composable
  override fun present(): RecipeDetailsState {
    val coroutineScope = rememberCoroutineScope()
    var retryTrigger by remember { mutableIntStateOf(0) }
    val state =
      produceRetainedContentState<Recipe?>(null, retryTrigger) {
        recipeRepository.getById(screen.id)
      }
    // Unlike the old response-shaped state, this keeps the recipe on screen through a refresh
    // rather than blanking it whenever the store goes back to the network.
    val recipe: Recipe? = state.data
    return RecipeDetailsState(
      recipe,
      loading = state.isLoading,
      error = state.errorOrNull != null,
    ) { event ->
      when (event) {
        RecipeDetailsEvent.ToggleFavorite ->
          coroutineScope.launch {
            if (recipe != null) {
              if (recipe.favorite) {
                recipeRepository.removeFavorite(screen.id)
              } else {
                recipeRepository.addFavorite(screen.id)
              }
            }
          }
        RecipeDetailsEvent.RetryClicked -> retryTrigger++
        is RecipeDetailsEvent.CategoryClicked -> navigator.goTo(ByCategory(event.category))

        is RecipeDetailsEvent.AreaClicked -> navigator.goTo(ByArea(event.area))
      }
    }
  }
}

data class RecipeDetailsState(
  val recipe: Recipe?,
  val loading: Boolean,
  val error: Boolean,
  @Redacted val eventSink: (RecipeDetailsEvent) -> Unit,
) : CircuitUiState

sealed interface RecipeDetailsEvent : CircuitUiEvent {
  data object ToggleFavorite : RecipeDetailsEvent

  data object RetryClicked : RecipeDetailsEvent

  data class CategoryClicked(val category: String) : RecipeDetailsEvent

  data class AreaClicked(val area: String) : RecipeDetailsEvent
}

@CircuitSerializable(AppScope::class) data class RecipeDetailsScreen(val id: RecipeId) : Screen
