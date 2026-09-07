package com.scottolcott.recipe

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.scottolcott.recipe.domain.isCupertino
import com.scottolcott.recipe.domain.presenter.HomeScreen
import com.scottolcott.recipe.domain.presenter.NavigationLayout
import com.scottolcott.recipe.domain.presenter.RecipeScaffoldEvent
import com.scottolcott.recipe.domain.presenter.RecipeScaffoldScreen
import com.scottolcott.recipe.domain.presenter.RecipeScaffoldState
import com.scottolcott.recipe.domain.presenter.RecipesScreen
import com.scottolcott.recipe.domain.presenter.SearchTabScreen
import com.scottolcott.recipe.ui.LocalFloatingBarInset
import com.scottolcott.recipe.ui.Res
import com.scottolcott.recipe.ui.chef_hat_24px
import com.scottolcott.recipe.ui.design.AppDestination
import com.scottolcott.recipe.ui.design.AppNavigationBar
import com.scottolcott.recipe.ui.design.LocalTopAppBarScrollBehavior
import com.scottolcott.recipe.ui.favorite_24px
import com.scottolcott.recipe.ui.favorite_24px_filled
import com.scottolcott.recipe.ui.favorites
import com.scottolcott.recipe.ui.maxContentWidth
import com.scottolcott.recipe.ui.recipes
import com.scottolcott.recipe.ui.search
import com.scottolcott.recipe.ui.search_24px
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.sharedelements.SharedElementTransitionLayout
import com.slack.circuitx.gesturenavigation.GestureNavigationDecorationFactory
import dev.zacsweers.metro.AppScope
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@CircuitInject(RecipeScaffoldScreen::class, AppScope::class)
@Composable
@Suppress("unused")
fun RecipeScaffoldScreen(state: RecipeScaffoldState, modifier: Modifier = Modifier) {
  BrowserHistoryEffect(navStack = state.navStack, navigator = state.navigator)
  BackShortcutEffect(state)
  val railDestinations = rememberAppDestinations(includeSearch = false)
  val tabBarDestinations = rememberAppDestinations(includeSearch = true)
  val scrollBehavior = rememberCollapsingTitleBehavior(state.navStack.currentRecord?.screen)
  val collapsingTitle = state.appBarTitleCollapses()
  CompositionLocalProvider(
    LocalTopAppBarScrollBehavior provides scrollBehavior,
    LocalAppBarShowsScreenTitle provides state.appBarShowsScreenTitle(),
  ) {
    Row(modifier.fillMaxSize()) {
      AnimatedVisibility(
        state.navigationLayout == NavigationLayout.Rail,
        enter = expandHorizontally(),
        exit = shrinkHorizontally(),
      ) {
        RecipeNavigationRail(state, railDestinations)
      }
      Scaffold(
        modifier =
          Modifier.weight(1f).let {
            // Nested scroll bubbles up from whichever screen is showing, so catching it here
            // means no screen has to know the bar exists.
            //
            // Only while that bar is actually on screen, though. The connection consumes an upward
            // delta for as long as the collapse has room left to run, and the room is
            // `heightOffsetLimit` -- which only a bar handed this behaviour ever measures onto the
            // state. With no such bar it stays at -Float.MAX_VALUE: the collapse never bottoms out,
            // every delta is swallowed, and nothing below scrolls. Silently, too, since an
            // unbounded limit also holds `collapsedFraction` at zero, so the bar does not even
            // appear to move. That is what froze every screen on a layout wide enough for the rail,
            // where the search field takes the whole bar and no large title is composed at all.
            if (scrollBehavior != null && collapsingTitle)
              it.nestedScroll(scrollBehavior.nestedScrollConnection)
            else it
          },
        topBar = { RecipeAppBar(state, modifier = Modifier.fillMaxWidth()) },
        bottomBar = { ScaffoldBottomBar(state, tabBarDestinations) },
        contentWindowInsets = WindowInsets(0.dp),
      ) { paddingValues ->
        val layoutDirection = LocalLayoutDirection.current
        // The bottom inset is handed to the screens rather than cut out of the content box. The
        // Cupertino tab bar is a capsule floating *over* the page and narrower than the window, so
        // reserving its height here would leave a dead strip beneath every screen and strand the
        // capsule in it. Screens add it to the bottom of their scroll padding instead, which lets a
        // list run under the capsule and past either side of it; see [LocalFloatingBarInset]. Every
        // other edge is applied here as usual.
        CompositionLocalProvider(
          LocalFloatingBarInset provides paddingValues.calculateBottomPadding()
        ) {
          Box(
            Modifier.fillMaxSize()
              .padding(
                start = paddingValues.calculateStartPadding(layoutDirection),
                top = paddingValues.calculateTopPadding(),
                end = paddingValues.calculateEndPadding(layoutDirection),
              )
          ) {
            SharedElementTransitionLayout {
              NavigableCircuitContent(
                navigator = state.navigator,
                navStack = state.navStack,
                decoratorFactory =
                  remember(state.navigator) { GestureNavigationDecorationFactory() },
                modifier = Modifier.fillMaxHeight().maxContentWidth(),
              )
            }
          }
        }
      }
    }
  }
}

/**
 * The scroll behaviour the Cupertino large title collapses against, or `null` under Material.
 *
 * The reset is why this is not a one-liner at the call site: `TopAppBarState` outlives a
 * navigation, so a title left collapsed by scrolling one screen would arrive already shrunk on the
 * next, which reads as a rendering fault rather than a transition.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun rememberCollapsingTitleBehavior(currentScreen: Screen?): TopAppBarScrollBehavior? {
  // Only the Cupertino bar collapses, so Material pays for none of this.
  val scrollBehavior =
    if (isCupertino) TopAppBarDefaults.exitUntilCollapsedScrollBehavior() else null
  // A keyed effect rather than LaunchedEffect: nothing here suspends, it just has to run once per
  // screen change rather than on every recomposition.
  DisposableEffect(scrollBehavior, currentScreen) {
    scrollBehavior?.state?.heightOffset = 0f
    scrollBehavior?.state?.contentOffset = 0f
    onDispose {}
  }
  return scrollBehavior
}

/** The tab bar, growing and shrinking the slot rather than popping in and out of it. */
@Composable
private fun ScaffoldBottomBar(state: RecipeScaffoldState, destinations: List<AppDestination>) {
  AnimatedVisibility(
    state.navigationLayout == NavigationLayout.BottomBar,
    enter = expandVertically(),
    exit = shrinkVertically(),
  ) {
    AppNavigationBar(
      destinations = destinations,
      selected = state.selectedDestination,
      onSelect = { state.eventSink(RecipeScaffoldEvent.SelectDestination(it)) },
    )
  }
}

/**
 * The sections the rail and the tab bar offer.
 *
 * One builder for both so the two cannot drift apart as sections are added. They differ in exactly
 * one entry: [includeSearch]. A rail layout keeps the search field docked in the app bar beside it,
 * so a Search destination there would point at a field already on screen; a tab bar has no room for
 * that field, so Search becomes somewhere you go instead.
 */
@Composable
private fun rememberAppDestinations(includeSearch: Boolean): List<AppDestination> =
  remember(includeSearch) {
    buildList {
      add(
        AppDestination(
          screen = HomeScreen(),
          icon = Res.drawable.chef_hat_24px,
          selectedIcon = Res.drawable.chef_hat_24px,
          label = Res.string.recipes,
        )
      )
      add(
        AppDestination(
          screen = RecipesScreen.Favorites,
          icon = Res.drawable.favorite_24px,
          selectedIcon = Res.drawable.favorite_24px_filled,
          label = Res.string.favorites,
        )
      )
      if (includeSearch) {
        add(
          AppDestination(
            screen = SearchTabScreen,
            icon = Res.drawable.search_24px,
            selectedIcon = Res.drawable.search_24px,
            label = Res.string.search,
          )
        )
      }
    }
  }

@Composable
private fun RecipeNavigationRail(
  state: RecipeScaffoldState,
  destinations: List<AppDestination>,
  modifier: Modifier = Modifier,
) {
  // Selection follows the section the current screen sits under, not the current record itself, so
  // a recipe opened from Favorites keeps Favorites lit rather than clearing the rail entirely.
  NavigationRail(
    modifier = modifier.fillMaxHeight(),
    header = {},
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
  ) {
    destinations.forEach { destination ->
      val selected = destination.isSelectedBy(state.selectedDestination)
      NavigationRailItem(
        selected = selected,
        onClick = { state.eventSink(RecipeScaffoldEvent.SelectDestination(destination.screen)) },
        icon = {
          Icon(
            painter = painterResource(destination.iconFor(selected)),
            contentDescription = null,
          )
        },
        label = { Text(stringResource(destination.label)) },
        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand).align(Alignment.Start),
      )
    }
  }
}

/**
 * Hands the window's keyboard shortcuts something to pop.
 *
 * A no-op wherever no [BackShortcutHost] was supplied, which is every platform but desktop.
 */
@Composable
private fun BackShortcutEffect(state: RecipeScaffoldState) {
  val host = LocalBackShortcutHost.current ?: return
  DisposableEffect(host, state) {
    host.onBack = {
      // Reports whether it moved, so at the root the key falls through unconsumed rather than
      // being swallowed.
      state.canGoBack.also { if (it) state.eventSink(RecipeScaffoldEvent.Back) }
    }
    onDispose { host.onBack = null }
  }
}
