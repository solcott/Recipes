package com.scottolcott.recipe.domain

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Which visual language the app is speaking.
 *
 * Screens branch on this rather than on [com.scottolcott.recipe.isIos], so that the iOS look is a
 * *design* the composition carries rather than a fact about the machine it is running on. Two
 * things fall out of that. `@Preview`s and `:desktopApp:hotRun` can render the iOS design by
 * providing [LocalAppDesign] directly, which is the difference between iterating on iOS chrome in a
 * hot-reload loop and rebuilding through Xcode for every nudge. And the one place that maps
 * platform to design stays a single line in [com.scottolcott.recipe.ui.theme.RecipeAppTheme],
 * instead of `isIos()` spreading through screen code the way it had begun to.
 *
 * `staticCompositionLocalOf` because this is fixed for the life of the composition -- nothing reads
 * it conditionally, so there is nothing to gain from tracking reads.
 */
enum class AppDesign {
  /** Material 3, as everything but iOS gets. */
  Material,

  /** Apple's design language: system colors, the SF type ramp, grouped lists, no ripple. */
  Cupertino,
}

val LocalAppDesign = staticCompositionLocalOf { AppDesign.Material }

/** Shorthand for the branch that appears at nearly every call site. */
val isCupertino: Boolean
  @Composable @ReadOnlyComposable get() = LocalAppDesign.current == AppDesign.Cupertino
