package com.scottolcott.recipe.ui

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import com.scottolcott.recipe.LocalWindowSizeClass

@Composable
fun rememberAdaptiveGridCells(): GridCells {
  val windowSizeClass = LocalWindowSizeClass.current
  return remember(windowSizeClass) {
    val minCellWidth =
      if (windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)) {
        250.dp
      } else {
        175.dp
      }
    GridCells.Adaptive(minCellWidth)
  }
}
