package com.scottolcott.recipe.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass.Companion.HEIGHT_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.HEIGHT_DP_MEDIUM_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
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

/**
 * Caps content at [WIDTH_DP_LARGE_LOWER_BOUND] and centers it in whatever space is left over,
 * filling the width below that.
 *
 * The cap is the large-window breakpoint so the rule reads "content never grows past a large
 * window; the extra room becomes margin". Past it the grid keeps adding columns with nothing to
 * anchor the eye, the tab row strands three tabs across the display, and the detail screen's
 * instructions track runs well past a readable line length.
 *
 * This is for the navigable content only. The top bar does not need it: Material3 already holds
 * `AppBarWithSearch` to its own 720dp maximum, and that bar centers on the same axis either way.
 *
 * `wrapContentWidth` sits between the two `fillMaxWidth` calls on purpose: the first takes the
 * width the parent offers, `wrapContentWidth` re-measures the child against a relaxed minimum and
 * centers it within that width, `widthIn` clamps the child, and the last fills the clamped space.
 */
fun Modifier.maxContentWidth(): Modifier =
  fillMaxWidth()
    .wrapContentWidth(Alignment.CenterHorizontally)
    .widthIn(max = WIDTH_DP_LARGE_LOWER_BOUND.dp)
    .fillMaxWidth()

/**
 * How much room the chrome floating over the bottom of the content needs, or `0.dp` where none
 * floats there.
 *
 * The Cupertino tab bar is a capsule that hovers over the page rather than a band beside it, and it
 * is narrower than the window, so the scaffold hands its height down here instead of cutting it out
 * of the content box -- which would strand the capsule in a dead strip. A screen that scrolls adds
 * it to the bottom of its scroll padding: the last row then comes to rest clear of the capsule,
 * while everything above it passes under and to either side of the capsule on the way, which is the
 * iOS look. Screens padding with [rememberAdaptivePadding] get this for free.
 *
 * Zero under Material, which reserves the space for its bars in the usual way.
 */
val LocalFloatingBarInset = compositionLocalOf { 0.dp }

@Composable
fun rememberAdaptivePadding(): PaddingValues {
  val windowSizeClass = LocalWindowSizeClass.current
  val floatingBarInset = LocalFloatingBarInset.current
  return remember(windowSizeClass, floatingBarInset) {
    val isMediumWidth = windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)
    val isExpandedWidth = windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_EXPANDED_LOWER_BOUND)
    val isLargeWidth = windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_LARGE_LOWER_BOUND)

    val isMediumHeight = windowSizeClass.isHeightAtLeastBreakpoint(HEIGHT_DP_MEDIUM_LOWER_BOUND)
    val isExpandedHeight = windowSizeClass.isHeightAtLeastBreakpoint(HEIGHT_DP_EXPANDED_LOWER_BOUND)
    // Every branch differs from the one below it, or the tier is dead code -- expanded and medium
    // both returned 24dp until this ramp replaced them. The steps are small at the low end on
    // purpose: padding is subtracted from the width the grid measures itself against, so a big
    // step here can cost a column at the very width that just gained the room for one.
    //
    // Large is the terminal tier because [maxContentWidth] freezes the content column there. An
    // extra-large step would read the window while the column it pads no longer grows, so it would
    // only take width away from the grid -- the same failure, arriving from the other direction.
    val horizontal =
      when {
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
    PaddingValues(
      start = horizontal,
      top = vertical,
      end = horizontal,
      bottom = vertical + floatingBarInset,
    )
  }
}
