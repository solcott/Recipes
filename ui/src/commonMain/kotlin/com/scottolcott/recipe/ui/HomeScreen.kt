package com.scottolcott.recipe.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.scottolcott.recipe.domain.presenter.AreasScreen
import com.scottolcott.recipe.domain.presenter.CategoriesScreen
import com.scottolcott.recipe.domain.presenter.HomeEvent
import com.scottolcott.recipe.domain.presenter.HomeScreen
import com.scottolcott.recipe.domain.presenter.HomeState
import com.scottolcott.recipe.domain.presenter.IngredientsScreen
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.foundation.CircuitContent
import dev.zacsweers.metro.AppScope
import org.jetbrains.compose.resources.stringResource

@Composable
@CircuitInject(HomeScreen::class, AppScope::class)
fun HomeScreen(state: HomeState, modifier: Modifier = Modifier) {
  // rememberPagerState reads initialPage only when it first builds the state; only the pageCount
  // lambda is read on later compositions.
  val pagerState = rememberPagerState(state.selectedIndex) { state.tabScreens.size }
  // Tap a tab -> animate pager
  LaunchedEffect(state.selectedTabScreen) {
    if (pagerState.currentPage != state.selectedIndex) {
      pagerState.animateScrollToPage(state.selectedIndex)
    }
  }

  // Swipe pager -> update presenter's selected index. settledPage rather than currentPage: the
  // latter tracks the nearest page mid-scroll, so animating across two tabs would report the tab
  // it passes through and flicker the indicator.
  LaunchedEffect(pagerState) {
    snapshotFlow { pagerState.settledPage }
      .collect { page -> state.eventSink(HomeEvent.TabSelected(state.tabScreens[page])) }
  }
  Column(modifier = modifier.fillMaxSize()) {
    PrimaryTabRow(
      selectedTabIndex = state.selectedIndex,
      contentColor = MaterialTheme.colorScheme.onSurface,
      indicator = {
        TabRowDefaults.PrimaryIndicator(
          modifier = Modifier.tabIndicatorOffset(state.selectedIndex, matchContentSize = true),
          width = Dp.Unspecified,
          color = MaterialTheme.colorScheme.onSurface,
        )
      },
    ) {
      state.tabScreens.forEach { tab ->
        Tab(
          selected = tab == state.selectedTabScreen,
          onClick = { state.eventSink(HomeEvent.TabSelected(tab)) },
          text = {
            val label =
              when (tab) {
                AreasScreen -> stringResource(Res.string.areas)
                CategoriesScreen -> stringResource(Res.string.categories)
                IngredientsScreen -> stringResource(Res.string.ingredients)
              }
            Text(label)
          },
        )
      }
    }

    HorizontalPager(pagerState, modifier = Modifier.weight(1f).fillMaxWidth()) {
      CircuitContent(
        screen = state.tabScreens[it],
        modifier = Modifier.fillMaxSize(),
        navigator = state.navigator,
      )
    }
  }
}
