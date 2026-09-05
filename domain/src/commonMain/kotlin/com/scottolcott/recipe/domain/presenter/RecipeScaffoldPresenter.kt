package com.scottolcott.recipe.domain.presenter

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SearchBarValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
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
import com.slack.circuit.runtime.navigation.canGoBack
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
      windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) &&
        windowSizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND) &&
        !isIos()

    val searchBarValue = retain { mutableStateOf(SearchBarValue.Collapsed) }
    // Whether the user asked for search on a layout that does not show it permanently. Derived
    // rather than stored so a change to any input -- including a resize across the nav rail
    // breakpoint -- is reflected during composition, with no apply-phase write to snapshot state.
    val searchRequested = retain { mutableStateOf(false) }
    val searchVisible =
      showNavRail || searchBarValue.value == SearchBarValue.Expanded || searchRequested.value

    val canGoBack = navStack.canGoBack
    // The screen a back gesture lands on: the one directly beneath the current record.
    val previousScreen = navStack.screensFromCurrent().getOrNull(1)

    val eventSink =
      remember(childNavigator) {
        scaffoldEventSink(childNavigator, navStack, searchBarValue, searchRequested)
      }

    @Suppress("OPT_IN_USAGE")
    return RecipeScaffoldState(
      navStack,
      childNavigator,
      searchVisible,
      showNavRail,
      canGoBack,
      previousScreen,
      eventSink,
    )
  }
}

/**
 * The scaffold's event handling.
 *
 * A plain function rather than a lambda inside `present` so the `when` does not count against the
 * composable's cyclomatic complexity; the two pieces of search state arrive as their `MutableState`
 * holders instead of `by` delegates because this is not a composable and cannot read them.
 */
@OptIn(ExperimentalMaterial3Api::class)
private fun scaffoldEventSink(
  navigator: Navigator,
  navStack: NavStack<out NavStack.Record>,
  searchBarValue: MutableState<SearchBarValue>,
  searchRequested: MutableState<Boolean>,
): (RecipeScaffoldEvent) -> Unit = { event ->
  when (event) {
    RecipeScaffoldEvent.Back -> if (navStack.canGoBack) navigator.pop()
    is RecipeScaffoldEvent.GoTo -> {
      navigator.goTo(event.screen)
      searchRequested.value = false
    }
    RecipeScaffoldEvent.ExitSearch -> searchRequested.value = false
    RecipeScaffoldEvent.SearchClicked -> searchRequested.value = true
    is RecipeScaffoldEvent.SearchBarStateChanged -> {
      searchBarValue.value = event.searchBarValue
      // Collapsing the bar is how the user leaves search, so it has to clear the request too.
      // Without this the derivation would hold searchVisible true after the bar closes and strand
      // a collapsed search bar where the top app bar belongs.
      if (event.searchBarValue == SearchBarValue.Collapsed) searchRequested.value = false
    }
  }
}

/**
 * The screens from the active record toward the root, current first.
 *
 * `backwardItems` is already ordered that way, so the current screen just goes in front of it.
 */
private fun NavStack<out NavStack.Record>.screensFromCurrent(): List<Screen> =
  listOfNotNull(currentRecord?.screen) + snapshot()?.backwardItems?.map { it.screen }.orEmpty()

sealed interface RecipeScaffoldEvent : CircuitUiEvent {
  data class GoTo(val screen: Screen) : RecipeScaffoldEvent

  data object Back : RecipeScaffoldEvent

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
  val canGoBack: Boolean,
  val previousScreen: Screen?,
  @Redacted val eventSink: (RecipeScaffoldEvent) -> Unit,
) : CircuitUiState

@CircuitSerializable(AppScope::class) data object RecipeScaffoldScreen : Screen
