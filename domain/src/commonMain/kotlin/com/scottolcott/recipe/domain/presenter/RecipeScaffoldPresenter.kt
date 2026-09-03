package com.scottolcott.recipe.domain.presenter

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SearchBarValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.window.core.layout.WindowSizeClass
import com.scottolcott.recipe.domain.LocalWindowSizeClass
import com.scottolcott.recipe.domain.navigation.LocalDeepLinkScreen
import com.scottolcott.recipe.isIos
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
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun present(): RecipeScaffoldState {
    val deepLinkScreen = LocalDeepLinkScreen.current
    val initialScreens = remember {
      buildList {
        // A deep link to a home tab selects that tab on the root record rather than stacking a
        // second screen on top of it.
        add(deepLinkScreen as? HomeScreen ?: HomeScreen())
        if (deepLinkScreen != null && deepLinkScreen !is HomeScreen) add(deepLinkScreen)
      }
    }
    val navStack = rememberSaveableNavStack(initialScreens)
    val childNavigator = rememberCircuitNavigator(navStack) { navigator.pop() }

    val windowSizeClass = LocalWindowSizeClass.current
    val showNavRail =
      windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) &&
        !isIos()

    var searchBarValue by retain { mutableStateOf(SearchBarValue.Collapsed) }
    var searchVisible by retain { mutableStateOf(false) }
    SideEffect(showNavRail, searchBarValue) {
      searchVisible = showNavRail || searchBarValue == SearchBarValue.Expanded
    }

    val eventSink: (RecipeScaffoldEvent) -> Unit =
      remember(childNavigator) {
        { event ->
          when (event) {
            is RecipeScaffoldEvent.GoTo -> {
              childNavigator.goTo(event.screen)
              searchVisible = showNavRail
            }
            RecipeScaffoldEvent.ExitSearch -> searchVisible = showNavRail
            RecipeScaffoldEvent.SearchClicked -> searchVisible = true
            is RecipeScaffoldEvent.SearchBarStateChanged -> searchBarValue = event.searchBarValue
          }
        }
      }

    @Suppress("OPT_IN_USAGE")
    return RecipeScaffoldState(navStack, childNavigator, searchVisible, showNavRail, eventSink)
  }
}

sealed interface RecipeScaffoldEvent : CircuitUiEvent {
  data class GoTo(val screen: Screen) : RecipeScaffoldEvent

  data object ExitSearch : RecipeScaffoldEvent

  data object SearchClicked : RecipeScaffoldEvent

  data class SearchBarStateChanged
  @OptIn(ExperimentalMaterial3Api::class)
  constructor(val searchBarValue: SearchBarValue) : RecipeScaffoldEvent
}

data class RecipeScaffoldState(
  val navStack: NavStack<out NavStack.Record>,
  val navigator: Navigator,
  val searchVisible: Boolean,
  val showNavRail: Boolean,
  @Redacted val eventSink: (RecipeScaffoldEvent) -> Unit,
) : CircuitUiState

@CircuitSerializable(AppScope::class) data object RecipeScaffoldScreen : Screen
