package com.scottolcott.recipe

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scottolcott.recipe.domain.presenter.RecipeScaffoldScreen
import com.scottolcott.recipe.domain.presenter.RecipeScaffoldState
import com.scottolcott.recipe.domain.presenter.SearchEvent
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.sharedelements.SharedElementTransitionLayout
import com.slack.circuitx.gesturenavigation.GestureNavigationDecorationFactory
import dev.zacsweers.metro.AppScope

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@CircuitInject(RecipeScaffoldScreen::class, AppScope::class)
@Composable
@Suppress("unused")
fun RecipeScaffoldScreen(state: RecipeScaffoldState, modifier: Modifier = Modifier) {
  Scaffold(
    modifier.fillMaxSize(),
    topBar = { RecipeAppBar(state, modifier = Modifier.fillMaxWidth()) },
    contentWindowInsets = WindowInsets(0.dp),
  ) { paddingValues ->
    Box(Modifier.fillMaxSize()) {
      SharedElementTransitionLayout {
        NavigableCircuitContent(
          navigator = state.navigator,
          navStack = state.navStack,
          decoratorFactory =
            remember(state.navigator) {
              GestureNavigationDecorationFactory(onBackInvoked = state.navigator::pop)
            },
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
