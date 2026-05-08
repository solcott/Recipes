package com.scottolcott.recipe.ui

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass.Companion.HEIGHT_DP_MEDIUM_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import com.scottolcott.recipe.LocalWindowSizeClass

@Composable
fun rememberAdaptiveGridCells(
  minWidthCompact: Dp = 175.dp,
  minWidthMedium: Dp = 250.dp,
  minWidthMediumCompact: Dp = minWidthMedium,
): GridCells {
  val windowSizeClass = LocalWindowSizeClass.current
  return remember(windowSizeClass, minWidthCompact, minWidthMedium, minWidthMediumCompact) {
    val isMediumWidth = windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)
    val isCompactHeight = !windowSizeClass.isHeightAtLeastBreakpoint(HEIGHT_DP_MEDIUM_LOWER_BOUND)

    val minCellWidth =
      when {
        isMediumWidth && isCompactHeight -> minWidthMediumCompact
        isMediumWidth -> minWidthMedium
        else -> minWidthCompact
      }
    GridCells.Adaptive(minCellWidth)
  }
}
