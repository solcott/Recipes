package com.scottolcott.recipe.ui.area

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import com.scottolcott.recipe.domain.LocalWindowSizeClass
import com.scottolcott.recipe.domain.presenter.AreasEvent
import com.scottolcott.recipe.domain.presenter.AreasScreen
import com.scottolcott.recipe.domain.presenter.AreasState
import com.scottolcott.recipe.model.Area
import com.scottolcott.recipe.ui.ErrorDisplay
import com.scottolcott.recipe.ui.Res
import com.scottolcott.recipe.ui.design.AppCard
import com.scottolcott.recipe.ui.isShortWindow
import com.scottolcott.recipe.ui.no_categories_found
import com.scottolcott.recipe.ui.rememberAdaptiveGridCells
import com.scottolcott.recipe.ui.rememberAdaptivePadding
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.zacsweers.metro.AppScope
import org.jetbrains.compose.resources.stringResource

@Suppress("unused")
@Composable
@CircuitInject(AreasScreen::class, AppScope::class)
fun IngredientScreen(state: AreasState, modifier: Modifier = Modifier) {
  val cells = rememberAdaptiveGridCells(targetWidth = 165.dp, shortWindowTargetWidth = 200.dp)
  val padding = rememberAdaptivePadding()
  // A short window is starved of vertical space even when it is wide -- a landscape phone -- so the
  // larger label is held back for windows that are both wide and tall.
  val roomyLabel =
    LocalWindowSizeClass.current.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND) &&
      !isShortWindow()
  val labelTextStyle =
    if (roomyLabel) {
      MaterialTheme.typography.titleMediumEmphasized
    } else {
      MaterialTheme.typography.titleSmallEmphasized
    }
  Box(modifier, contentAlignment = Alignment.TopCenter) {
    when (state) {
      is AreasState.Error ->
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          ErrorDisplay(onRetryClick = { state.eventSink(AreasEvent.Error.RetryClicked) })
        }

      AreasState.Loading ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator()
        }

      is AreasState.Success -> {
        if (state.areas.isEmpty()) {
          Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(Res.string.no_categories_found))
          }
        } else {
          LazyVerticalGrid(
            cells,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = padding,
          ) {
            items(state.areas, key = { it.area }, contentType = { "category_item" }) {
              AreaItem(
                it,
                areasTextStyle = labelTextStyle,
                countryTextStyle = MaterialTheme.typography.titleSmall,
                { state.eventSink(AreasEvent.Success.AreaClicked(it.area)) },
                Modifier.animateItem().pointerHoverIcon(PointerIcon.Hand, true),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun AreaItem(
  category: Area,
  areasTextStyle: TextStyle,
  countryTextStyle: TextStyle,
  onCategoryClick: () -> Unit,
  modifier: Modifier = Modifier,
) {

  AppCard(onClick = onCategoryClick, modifier = modifier) {
    Column {
      Text(
        category.area,
        Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        style = areasTextStyle,
        maxLines = 1,
      )
      Text(
        category.country,
        Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        style = countryTextStyle,
        maxLines = 1,
      )
    }
  }
}
