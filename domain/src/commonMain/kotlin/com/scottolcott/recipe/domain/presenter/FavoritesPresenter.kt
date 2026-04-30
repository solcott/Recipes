package com.scottolcott.recipe.domain.presenter

import androidx.compose.runtime.Composable
import com.scottolcott.recipe.model.RecipeId
import com.scottolcott.recipe.repository.RecipeRepository
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import io.github.solcott.kmp.parcelize.Parcelize

@AssistedInject
class FavoritesPresenter(
  @Assisted private val navigator: Navigator,
  private val recipeRepository: RecipeRepository,
) : Presenter<FavoritesState> {
  @Composable
  override fun present(): FavoritesState {

    return FavoritesState(emptyList())
  }

  @CircuitInject(FavoritesScreen::class, AppScope::class)
  @AssistedFactory
  interface Factory {
    @Suppress("unused") fun create(navigator: Navigator): FavoritesPresenter
  }
}

data class FavoritesState(val recipes: List<RecipeId>) : CircuitUiState

@Parcelize data object FavoritesScreen : Screen
