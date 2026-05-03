package com.scottolcott.recipe.domain.presenter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import co.touchlab.kermit.Logger
import com.scottolcott.recipe.isError
import com.scottolcott.recipe.isLoading
import com.scottolcott.recipe.model.Recipe
import com.scottolcott.recipe.model.RecipeId
import com.scottolcott.recipe.repository.RecipeRepository
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.produceRetainedState
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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.mobilenativefoundation.store.store5.StoreReadResponse

@AssistedInject
class RecipeDetailsPresenter(
  @Assisted private val screen: Screen,
  @Assisted private val navigator: Navigator,
  private val recipeRepository: RecipeRepository,
  private val logger: Logger,
) : Presenter<RecipeDetailsState> {
  @Composable
  override fun present(): RecipeDetailsState {
    val coroutineScope = rememberCoroutineScope()
    var retryTrigger by remember { mutableIntStateOf(0) }
    screen as RecipeDetailsScreen
    val recipeResponse by
      produceRetainedState<StoreReadResponse<Recipe?>>(StoreReadResponse.Initial, retryTrigger) {
        recipeRepository
          .getById(screen.id)
          .onEach {
            when (it) {
              is StoreReadResponse.Error.Exception -> logger.e(it.error) { "Error loading recipe" }

              is StoreReadResponse.Error.Message ->
                logger.e { "Error loading recipe: ${it.message}" }

              else -> Unit
            }
          }
          .collect { value = it }
      }
    val recipe: Recipe? = (recipeResponse as? StoreReadResponse.Data<Recipe?>)?.value
    return RecipeDetailsState(
      recipe,
      loading = recipeResponse.isLoading,
      error = recipeResponse.isError,
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
      }
    }
  }

  @CircuitInject(RecipeDetailsScreen::class, AppScope::class)
  @AssistedFactory
  interface Factory {
    @Suppress("unused") fun create(screen: Screen, navigator: Navigator): RecipeDetailsPresenter
  }
}

data class RecipeDetailsState(
  val recipe: Recipe?,
  val loading: Boolean,
  val error: Boolean,
  @Redacted val eventSink: (RecipeDetailsEvent) -> Unit,
) : CircuitUiState

sealed class RecipeDetailsEvent : CircuitUiEvent {
  data object ToggleFavorite : RecipeDetailsEvent()
}

@Parcelize data class RecipeDetailsScreen(val id: RecipeId) : Screen
