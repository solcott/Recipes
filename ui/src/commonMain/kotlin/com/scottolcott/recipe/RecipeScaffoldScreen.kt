package com.scottolcott.recipe

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.scottolcott.recipe.domain.presenter.HomeScreen
import com.scottolcott.recipe.domain.presenter.RecipeScaffoldEvent
import com.scottolcott.recipe.domain.presenter.RecipeScaffoldScreen
import com.scottolcott.recipe.domain.presenter.RecipeScaffoldState
import com.scottolcott.recipe.domain.presenter.RecipesScreen
import com.scottolcott.recipe.ui.Res
import com.scottolcott.recipe.ui.chef_hat_24px
import com.scottolcott.recipe.ui.favorite_24px
import com.scottolcott.recipe.ui.favorite_24px_filled
import com.scottolcott.recipe.ui.favorites
import com.scottolcott.recipe.ui.maxContentWidth
import com.scottolcott.recipe.ui.recipes
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.foundation.NavigableCircuitContent
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
  Row(modifier.fillMaxSize()) {
    AnimatedVisibility(
      state.showNavRail,
      enter = expandHorizontally(),
      exit = shrinkHorizontally(),
    ) {
      RecipeNavigationRail(state)
    }
    Scaffold(
      modifier = Modifier.weight(1f),
      topBar = { RecipeAppBar(state, modifier = Modifier.fillMaxWidth()) },
      contentWindowInsets = WindowInsets(0.dp),
    ) { paddingValues ->
      Box(Modifier.fillMaxSize().padding(paddingValues)) {
        SharedElementTransitionLayout {
          NavigableCircuitContent(
            navigator = state.navigator,
            navStack = state.navStack,
            decoratorFactory = remember(state.navigator) { GestureNavigationDecorationFactory() },
            modifier = Modifier.fillMaxHeight().maxContentWidth(),
          )
        }
      }
    }
  }
}

@Composable
private fun RecipeNavigationRail(state: RecipeScaffoldState, modifier: Modifier = Modifier) {
  NavigationRail(
    modifier = modifier.fillMaxHeight(),
    header = {},
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
  ) {
    NavigationRailItem(
      selected = state.navStack.currentRecord?.screen is HomeScreen,
      onClick = { state.eventSink(RecipeScaffoldEvent.GoTo(HomeScreen())) },
      icon = {
        Icon(painter = painterResource(Res.drawable.chef_hat_24px), contentDescription = null)
      },
      label = { Text(stringResource(Res.string.recipes)) },
      modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
    )
    NavigationRailItem(
      selected = state.navStack.currentRecord?.screen == RecipesScreen.Favorites,
      onClick = { state.eventSink(RecipeScaffoldEvent.GoTo(RecipesScreen.Favorites)) },
      icon = {
        Icon(
          painter =
            painterResource(
              if (state.navStack.currentRecord?.screen == RecipesScreen.Favorites) {
                Res.drawable.favorite_24px_filled
              } else {
                Res.drawable.favorite_24px
              }
            ),
          contentDescription = null,
        )
      },
      label = { Text(stringResource(Res.string.favorites)) },
      modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
    )
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
