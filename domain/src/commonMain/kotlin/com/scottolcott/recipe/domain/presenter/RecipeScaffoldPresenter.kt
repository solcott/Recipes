package com.scottolcott.recipe.domain.presenter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.scottolcott.recipe.domain.navigation.LocalDeepLinkScreen
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.foundation.navstack.rememberSaveableNavStack
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.navigation.NavStack
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.serialization.CircuitSerializable
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.redacted.annotations.Redacted

@CircuitInject(RecipeScaffoldScreen::class, AppScope::class)
@Inject
class RecipeScaffoldPresenter internal constructor(private val navigator: Navigator) :
  Presenter<RecipeScaffoldState> {
  @Composable
  override fun present(): RecipeScaffoldState {
    val deepLinkScreen = LocalDeepLinkScreen.current
    val initialScreens = remember {
      buildList {
        add(CategoriesScreen)
        if (deepLinkScreen != null && deepLinkScreen != CategoriesScreen) add(deepLinkScreen)
      }
    }
    val navStack = rememberSaveableNavStack(initialScreens)
    val childNavigator = rememberCircuitNavigator(navStack) { navigator.pop() }

    var searchActive by rememberSaveable { mutableStateOf(false) }

    val eventSink: (RecipeScaffoldEvent) -> Unit = remember {
      { event ->
        when (event) {
          is RecipeScaffoldEvent.GoTo -> {
            childNavigator.goTo(event.screen)
            searchActive = false
          }
          RecipeScaffoldEvent.ExitSearch -> searchActive = false
          RecipeScaffoldEvent.SearchClicked -> searchActive = true
        }
      }
    }

    @Suppress("OPT_IN_USAGE")
    return RecipeScaffoldState(navStack, childNavigator, searchActive, eventSink)
  }
}

sealed interface RecipeScaffoldEvent : CircuitUiEvent {
  data class GoTo(val screen: Screen) : RecipeScaffoldEvent

  data object ExitSearch : RecipeScaffoldEvent

  data object SearchClicked : RecipeScaffoldEvent
}

data class RecipeScaffoldState(
  val navStack: NavStack<out NavStack.Record>,
  val navigator: Navigator,
  val isSearchActive: Boolean,
  @Redacted val eventSink: (RecipeScaffoldEvent) -> Unit,
) : CircuitUiState

@CircuitSerializable(AppScope::class) data object RecipeScaffoldScreen : Screen
