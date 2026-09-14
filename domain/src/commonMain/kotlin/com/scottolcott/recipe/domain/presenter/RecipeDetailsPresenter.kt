package com.scottolcott.recipe.domain.presenter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.retain.retain
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
    var retryTrigger by retain { mutableIntStateOf(0) }
    val state =
      produceRetainedContentState<Recipe?>(null, retryTrigger) {
        recipeRepository.getById(screen.id)
      }
    // Read through updated state: the sink below is remembered once, so closing over a plain local
    // would pin it to the recipe from the first composition -- which is always null.
    val latestRecipe by rememberUpdatedState(state.data)
    val successEventSink: (RecipeDetailsEvent.Success) -> Unit = remember {
      { event ->
        when (event) {
          RecipeDetailsEvent.Success.ToggleFavorite ->
            coroutineScope.launch {
              val recipe = latestRecipe ?: return@launch
              if (recipe.favorite) {
                recipeRepository.removeFavorite(screen.id)
              } else {
                recipeRepository.addFavorite(screen.id)
              }
            }
          is RecipeDetailsEvent.Success.CategoryClicked ->
            navigator.goTo(ByCategory(event.category))

          is RecipeDetailsEvent.Success.AreaClicked -> navigator.goTo(ByArea(event.area))
        }
      }
    }

    val errorEventSink: (RecipeDetailsEvent.Error) -> Unit = remember {
      { event ->
        when (event) {
          RecipeDetailsEvent.Error.RetryClicked -> retryTrigger++
        }
      }
    }
    return state.foldToState(
      onLoading = { RecipeDetailsState.Loading },
      onError = { message -> RecipeDetailsState.Error(message, errorEventSink) },
      onContent = { recipe, isRefreshing ->
        RecipeDetailsState.Success(recipe, isRefreshing, successEventSink)
      },
    )
  }
}

sealed interface RecipeDetailsState : CircuitUiState {
  data object Loading : RecipeDetailsState

  data class Error(
    val message: String,
    @Redacted val eventSink: (RecipeDetailsEvent.Error) -> Unit,
  ) : RecipeDetailsState

  data class Success(
    val recipe: Recipe,
    val isRefreshing: Boolean,
    @Redacted val eventSink: (RecipeDetailsEvent.Success) -> Unit,
  ) : RecipeDetailsState
}

sealed interface RecipeDetailsEvent : CircuitUiEvent {
  sealed interface Success : RecipeDetailsEvent {
    data class CategoryClicked(val category: String) : Success

    data class AreaClicked(val area: String) : Success

    data object ToggleFavorite : Success
  }

  sealed interface Error : RecipeDetailsEvent {
    data object RetryClicked : Error
  }
}

@CircuitSerializable(AppScope::class) data class RecipeDetailsScreen(val id: RecipeId) : Screen
