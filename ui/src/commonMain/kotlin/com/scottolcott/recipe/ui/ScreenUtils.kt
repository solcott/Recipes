package com.scottolcott.recipe.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass.Companion.HEIGHT_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.HEIGHT_DP_MEDIUM_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXTRA_LARGE_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_LARGE_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import com.scottolcott.recipe.domain.LocalWindowSizeClass

/**
 * Whether the window is too short to spend vertical space freely -- a landscape phone, or a squat
 * desktop window.
 *
 * The single definition of that case, so the layouts that react to it stay in agreement: it picks
 * the horizontal recipe card and the wider grid cell that card needs.
 */
@Composable
fun isShortWindow(): Boolean =
  !LocalWindowSizeClass.current.isHeightAtLeastBreakpoint(HEIGHT_DP_MEDIUM_LOWER_BOUND)

@Composable
fun rememberAdaptivePadding(): PaddingValues {
  val windowSizeClass = LocalWindowSizeClass.current
  return remember(windowSizeClass) {
    val isMediumWidth = windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)
    val isExpandedWidth = windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_EXPANDED_LOWER_BOUND)
    val isLargeWidth = windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_LARGE_LOWER_BOUND)
    val isExtraLargeWidth =
      windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_EXTRA_LARGE_LOWER_BOUND)

    val isMediumHeight = windowSizeClass.isHeightAtLeastBreakpoint(HEIGHT_DP_MEDIUM_LOWER_BOUND)
    val isExpandedHeight = windowSizeClass.isHeightAtLeastBreakpoint(HEIGHT_DP_EXPANDED_LOWER_BOUND)
    // Every branch differs from the one below it, or the tier is dead code -- expanded and medium
    // both returned 24dp until this ramp replaced them. The steps are small at the low end on
    // purpose: padding is subtracted from the width the grid measures itself against, so a big
    // step here can cost a column at the very width that just gained the room for one.
    val horizontal =
      when {
        isExtraLargeWidth -> 48.dp
        isLargeWidth -> 32.dp
        isExpandedWidth -> 24.dp
        isMediumWidth -> 20.dp
        else -> 16.dp
      }

    val vertical =
      when {
        // If not at least medium width then most likely a phone. Deliberately first, so it
        // shadows both height branches: a tall phone should keep phone padding rather than pick
        // up the roomier spacing meant for a tall window on a larger screen.
        !isMediumWidth -> 16.dp
        isExpandedHeight -> 32.dp
        isMediumHeight -> 24.dp
        else -> 16.dp
      }
    PaddingValues(horizontal = horizontal, vertical = vertical)
  }
}
