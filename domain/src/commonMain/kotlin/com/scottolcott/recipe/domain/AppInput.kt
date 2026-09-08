package com.scottolcott.recipe.domain

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * What kind of machine is driving the app: a finger, or a pointer and a keyboard.
 *
 * A second axis beside [AppDesign], not more arms on it. The two answer different questions and
 * compose: `-Pdesign=cupertino -Pinput=touch` is the iOS design on a desktop run, which is the only
 * way to iterate iOS chrome without an Xcode round-trip, and folding both into one enum would take
 * that away. It would also turn every two-way `isCupertino` branch in `:ui` into a decision.
 * Desktop is not a third visual language; it is Material at a density a mouse can hit.
 *
 * Screens branch on this rather than on [com.scottolcott.recipe.isDesktop], for the reason
 * [AppDesign] gives: the design is something the composition carries, so a `@Preview` or a
 * `:desktopApp:hotRun` can render either one, and the single line that maps platform to input stays
 * in [com.scottolcott.recipe.RecipeApp] instead of spreading through screen code.
 *
 * Web takes [Pointer] by default, which a tablet browser does not deserve -- that is why
 * `RecipeApp` takes this as a parameter, so `webApp` can hand it [Touch] on a coarse pointer.
 *
 * `staticCompositionLocalOf` because this is fixed for the life of the composition, same as
 * [LocalAppDesign].
 */
enum class AppInput {
  /** A finger on glass: phone, tablet, and any browser reporting a coarse pointer. */
  Touch,

  /** A pointer and a keyboard, with hover and tooltips to spend and smaller targets to hit. */
  Pointer,
}

val LocalAppInput = staticCompositionLocalOf { AppInput.Touch }

/** Shorthand for the branch that appears at nearly every call site. */
val isPointer: Boolean
  @Composable @ReadOnlyComposable get() = LocalAppInput.current == AppInput.Pointer
