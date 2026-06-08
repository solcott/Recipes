package com.scottolcott.recipe

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.scottolcott.recipe.domain.presenter.CategoriesScreen
import com.scottolcott.recipe.domain.presenter.RecipeScaffoldEvent
import com.scottolcott.recipe.domain.presenter.RecipeScaffoldScreen
import com.scottolcott.recipe.domain.presenter.RecipeScaffoldState
import com.scottolcott.recipe.domain.presenter.RecipesScreen
import com.scottolcott.recipe.domain.presenter.SearchEvent
import com.scottolcott.recipe.ui.Res
import com.scottolcott.recipe.ui.chef_hat_24px
import com.scottolcott.recipe.ui.favorite_24px
import com.scottolcott.recipe.ui.favorite_24px_filled
import com.scottolcott.recipe.ui.favorites
import com.scottolcott.recipe.ui.recipes
import com.scottolcott.recipe.ui.search
import com.scottolcott.recipe.ui.search_24px
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
  val windowSizeClass = LocalWindowSizeClass.current
  val showNavRail =
    windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) &&
      !isIos()

  Row(modifier.fillMaxSize()) {
    if (showNavRail) {
      RecipeNavigationRail(state)
    }
    Scaffold(
      modifier = Modifier.weight(1f),
      topBar = {
        if (!showNavRail || state.searchState.isSearchActive) {
          RecipeAppBar(state, modifier = Modifier.fillMaxWidth())
        }
      },
      contentWindowInsets = WindowInsets(0.dp),
    ) { paddingValues ->
      Box(Modifier.fillMaxSize()) {
        SharedElementTransitionLayout {
          NavigableCircuitContent(
            navigator = state.navigator,
            navStack = state.navStack,
            decoratorFactory = remember(state.navigator) { GestureNavigationDecorationFactory() },
            modifier = Modifier.fillMaxSize().padding(paddingValues),
          )
        }
        if (state.searchState.isSearchActive) {
          Box(
            Modifier.fillMaxSize()
              .clickable(
                interactionSource = null,
                indication = null,
                onClick = { state.searchState.eventSink(SearchEvent.ExitSearch) },
              )
          )
        }
      }
    }
  }
}

@Composable
private fun RecipeNavigationRail(state: RecipeScaffoldState, modifier: Modifier = Modifier) {
  NavigationRail(modifier = modifier.fillMaxHeight()) {
    NavigationRailItem(
      selected = state.navStack.currentRecord?.screen == CategoriesScreen,
      onClick = { state.eventSink(RecipeScaffoldEvent.GoTo(CategoriesScreen)) },
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
    NavigationRailItem(
      selected = state.searchState.isSearchActive,
      onClick = { state.searchState.eventSink(SearchEvent.SearchButtonClicked) },
      icon = {
        Icon(painter = painterResource(Res.drawable.search_24px), contentDescription = null)
      },
      label = { Text(stringResource(Res.string.search)) },
      modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
    )
  }
}
