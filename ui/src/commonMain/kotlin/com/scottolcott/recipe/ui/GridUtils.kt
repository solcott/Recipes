package com.scottolcott.recipe.ui

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.roundToInt

/** How much a cell grows per 1000dp of grid width. See [TargetWidthCells]. */
private const val GROWTH_PER_DP = 0.02f

/**
 * [GridCells] that keeps every cell as close as it can to a target width.
 *
 * [targetWidth] is a target, not a minimum. Cells are laid out at the count nearest the target, so
 * a cell lands within a half column of it -- 0.75x to 1.25x at two columns -- rather than the 1x to
 * 2x spread of [GridCells.Adaptive], which floors the count and so drops a whole column the moment
 * two minimum-width cells no longer fit.
 *
 * [shortWindowTargetWidth] applies when [isShortWindow] is true, for screens whose cards switch to
 * a wider, shorter design there.
 */
@Composable
fun rememberAdaptiveGridCells(
  targetWidth: Dp = 175.dp,
  shortWindowTargetWidth: Dp = targetWidth,
): GridCells {
  val isShortWindow = isShortWindow()
  return remember(isShortWindow, targetWidth, shortWindowTargetWidth) {
    TargetWidthCells(if (isShortWindow) shortWindowTargetWidth else targetWidth)
  }
}

/**
 * A data class so it carries the equals/hashCode a [GridCells] implementation is expected to have.
 */
@Immutable
private data class TargetWidthCells(private val targetWidth: Dp) : GridCells {
  override fun Density.calculateCrossAxisCellSizes(availableSize: Int, spacing: Int): List<Int> {
    // Cards grow with the room they are actually given, continuously, rather than stepping at a
    // window breakpoint. The window and the grid disagree by the width of the navigation rail and
    // the content padding, so a target that steps where the available width does not can *drop* a
    // column as the window grows -- which is what made narrowing past the rail breakpoint run
    // 3 columns -> 2 -> 3. Growing this slowly also keeps the column count monotonic in the
    // available width: d/dA of (A + spacing) / (target(A) + spacing) stays positive while the
    // growth rate is below 1.
    val target = targetWidth + availableSize.toDp() * GROWTH_PER_DP
    val slot = target.roundToPx() + spacing
    val columns = max(1, ((availableSize + spacing).toFloat() / slot).roundToInt())
    // Spread the leftover pixels over the leading cells rather than letting them fall off the
    // trailing edge, matching how Compose sizes its own grid cells.
    val gridSize = availableSize - spacing * (columns - 1)
    val cellSize = gridSize / columns
    val remainder = gridSize % columns
    return List(columns) { cellSize + if (it < remainder) 1 else 0 }
  }
}
