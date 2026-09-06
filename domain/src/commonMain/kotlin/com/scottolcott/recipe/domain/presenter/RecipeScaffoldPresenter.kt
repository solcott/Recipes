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
import com.slack.circuit.runtime.Navigator.StateOptions
import com.slack.circuit.runtime.navigation.NavStack
import com.slack.circuit.runtime.navigation.canGoBack
import com.slack.circuit.runtime.popUntil
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.serialization.CircuitSerializable
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.redacted.annotations.Redacted

/**
 * The sections the navigation rail switches between, and the roots the app-bar shortcuts lead back
 * to.
 *
 * Order is the rail's order. Matching against these goes through [isSameDestinationAs] rather than
 * equality -- see there for why [HomeScreen] cannot be compared directly.
 */
internal val TOP_LEVEL_DESTINATIONS: List<Screen> = listOf(HomeScreen(), RecipesScreen.Favorites)

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
    val selectedDestination = navStack.screensFromCurrent().selectedDestination()

    val eventSink =
      remember(childNavigator, showNavRail) {
        scaffoldEventSink(childNavigator, navStack, showNavRail, searchBarValue, searchRequested)
      }

    @Suppress("OPT_IN_USAGE")
    return RecipeScaffoldState(
      navStack,
      childNavigator,
      searchVisible,
      showNavRail,
      canGoBack,
      selectedDestination,
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
  showNavRail: Boolean,
  searchBarValue: MutableState<SearchBarValue>,
  searchRequested: MutableState<Boolean>,
): (RecipeScaffoldEvent) -> Unit = { event ->
  when (event) {
    RecipeScaffoldEvent.Back -> if (navStack.canGoBack) navigator.pop()
    is RecipeScaffoldEvent.GoTo -> {
      navigator.goTo(event.screen)
      searchRequested.value = false
    }
    is RecipeScaffoldEvent.SelectDestination -> {
      navigator.selectDestination(navStack, event.screen, canSwapRoot = showNavRail)
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
 * Switches to a top-level [destination] without ever stacking one section on top of another.
 *
 * The rail's sections are peers, so arriving at one should leave nothing to go back to: the
 * destination becomes the sole root. A plain `goTo` instead makes Favorites look like a child of
 * Recipes, with a back button in the app bar pointing at the very section the rail already offers.
 *
 * Three cases, each a single net change to the stack:
 * - Already inside the section: collapse it to its own root.
 * - No rail on screen ([canSwapRoot] false): push. The rail is what makes a root swap safe -- below
 *   its breakpoint the app-bar shortcut is the only way across, and swapping would strand the user
 *   with a system back that exits the app.
 * - Otherwise: collapse the section being left, then swap the root.
 *
 * The collapse before the swap is what keeps `StateOptions.SaveAndRestore` honest here. `resetRoot`
 * saves the whole current snapshot, so collapsing first means the saved stack is a single record
 * and returning lands on that section's root rather than wherever the user last wandered. What the
 * save buys is the record itself: `restoreState` returns the same `Record` instances, so
 * `HomePresenter` keeps the tab it had selected.
 *
 * Every branch is also a single net change for `ui/src/webMain/.../BrowserHistoryEffect.web.kt`,
 * which mirrors the stack into browser history -- it watches the root screen alongside the size and
 * depth deltas precisely because a swap is invisible in those two numbers alone.
 */
private fun Navigator.selectDestination(
  navStack: NavStack<out NavStack.Record>,
  destination: Screen,
  canSwapRoot: Boolean,
) {
  val screens = navStack.screensFromCurrent()
  when {
    // The guard is load-bearing: Navigator.popUntil pops the root when nothing matches, which fires
    // this navigator's onRootPop and pops the *outer* stack.
    screens.any { it.isSameDestinationAs(destination) } ->
      popUntil { it.isSameDestinationAs(destination) }
    !canSwapRoot -> goTo(destination)
    else -> {
      screens.lastOrNull()?.let { root -> popUntil { it == root } }
      resetRoot(navStack.savedRootFor(destination) ?: destination, StateOptions.SaveAndRestore)
    }
  }
}

/**
 * The exact screen a saved stack for [destination] is filed under, if there is one.
 *
 * `SaveableNavStack` keys saved state by the root record's screen, and `HomeScreen` is a data class
 * carrying the tab it was seeded with. A `/home/areas` deep link roots the stack at
 * `HomeScreen(AreasScreen)`, so asking `resetRoot` for the rail's plain `HomeScreen()` would miss
 * Home's own saved state and build a fresh record instead.
 */
private fun NavStack<out NavStack.Record>.savedRootFor(destination: Screen): Screen? =
  peekState().firstOrNull { it.isSameDestinationAs(destination) }

/**
 * Whether this screen belongs to the same top-level destination as [other].
 *
 * [HomeScreen] is a data class carrying the selected tab, so equality is the wrong test:
 * `HomeScreen(AreasScreen)` sitting in the stack is still the destination the rail's Recipes item
 * means. Every other destination is a `data object` and compares fine.
 */
internal fun Screen.isSameDestinationAs(other: Screen): Boolean =
  if (other is HomeScreen) this is HomeScreen else this == other

/**
 * The screens from the active record toward the root, current first.
 *
 * `backwardItems` is already ordered that way, so the current screen just goes in front of it.
 */
private fun NavStack<out NavStack.Record>.screensFromCurrent(): List<Screen> =
  listOfNotNull(currentRecord?.screen) + snapshot()?.backwardItems?.map { it.screen }.orEmpty()

/**
 * The top-level destination the current stack position sits under, for the rail's highlight.
 *
 * Receives the screens current-first (see [screensFromCurrent]), so a recipe opened from Favorites
 * still reports Favorites rather than falling through to the Home root beneath it.
 */
internal fun List<Screen>.selectedDestination(): Screen? = firstNotNullOfOrNull { screen ->
  TOP_LEVEL_DESTINATIONS.firstOrNull { screen.isSameDestinationAs(it) }
}

sealed interface RecipeScaffoldEvent : CircuitUiEvent {
  data class GoTo(val screen: Screen) : RecipeScaffoldEvent

  /** Switch to one of [TOP_LEVEL_DESTINATIONS]; see `Navigator.selectDestination`. */
  data class SelectDestination(val screen: Screen) : RecipeScaffoldEvent

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
  val selectedDestination: Screen?,
  @Redacted val eventSink: (RecipeScaffoldEvent) -> Unit,
) : CircuitUiState

@CircuitSerializable(AppScope::class) data object RecipeScaffoldScreen : Screen
