@file:Suppress("unused")

package com.scottolcott.recipe.domain.presenter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.scottolcott.recipe.repository.RecipeRepository
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.foundation.navstack.rememberSaveableNavStack
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.navigation.NavStack
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.redacted.annotations.Redacted
import io.github.solcott.kmp.parcelize.Parcelize

@AssistedInject
class RecipeScaffoldPresenter(
  @Assisted private val navigator: Navigator,
  private val recipeRepository: RecipeRepository,
) : Presenter<RecipeScaffoldState> {
  @Composable
  override fun present(): RecipeScaffoldState {
    val navStack = rememberSaveableNavStack(listOf(CategoriesScreen))
    val childNavigator = rememberCircuitNavigator(navStack) { navigator.pop() }
    // Have to create inline instead of injecting due to needing access to the childNavigator
    val searchPresenter =
      remember(childNavigator) { SearchPresenter(childNavigator, recipeRepository) }
    val searchState = searchPresenter.present()

    val eventSink: (RecipeScaffoldEvent) -> Unit = remember {
      { event ->
        when (event) {
          is RecipeScaffoldEvent.GoTo -> childNavigator.goTo(event.screen)
        }
      }
    }

    @Suppress("OPT_IN_USAGE")
    return RecipeScaffoldState(navStack, childNavigator, searchState, eventSink)
  }

  @CircuitInject(RecipeScaffoldScreen::class, AppScope::class)
  @AssistedFactory
  interface Factory {
    @Suppress("unused") fun create(navigator: Navigator): RecipeScaffoldPresenter
  }
}

sealed class RecipeScaffoldEvent : CircuitUiEvent {
  data class GoTo(val screen: Screen) : RecipeScaffoldEvent()
}

data class RecipeScaffoldState(
  val navStack: NavStack<out NavStack.Record>,
  val navigator: Navigator,
  val searchState: SearchState,
  @Redacted val eventSink: (RecipeScaffoldEvent) -> Unit,
) : CircuitUiState

@Parcelize data object RecipeScaffoldScreen : Screen
