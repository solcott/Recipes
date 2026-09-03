package com.scottolcott.recipe.domain

import androidx.compose.runtime.compositionLocalOf
import androidx.window.core.layout.WindowSizeClass

@Suppress("CompositionLocalAllowlist")
val LocalWindowSizeClass =
  compositionLocalOf<WindowSizeClass> { error("No WindowSizeClass provided") }
