package com.scottolcott.recipe.domain.presenter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import com.scottolcott.recipe.domain.producer.RecipesProducer
import com.scottolcott.recipe.errorMessage
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
import org.mobilenativefoundation.store.store5.StoreReadResponse

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
    val recipesResponse = produceRecipesResponse(screen, retryTrigger)

    var lastRecipes by retain(retryTrigger) { mutableStateOf<List<Recipe>?>(null) }
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

      is StoreReadResponse.Error -> RecipesState.Error(recipesResponse.errorMessage, errorEventSink)

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

  @Composable
  private fun produceRecipesResponse(
    screen: RecipesScreen,
    retryTrigger: Int,
  ): StoreReadResponse<List<Recipe>> {
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
