package com.scottolcott.recipe.ui.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import com.scottolcott.recipe.domain.AppDesign
import com.scottolcott.recipe.domain.AppInput
import com.scottolcott.recipe.domain.LocalAppDesign
import com.scottolcott.recipe.domain.LocalAppInput
import com.scottolcott.recipe.isDesktop
import com.scottolcott.recipe.isIos
import com.scottolcott.recipe.isWeb
import com.scottolcott.recipe.ui.design.CupertinoFadeIndication
import com.scottolcott.recipe.ui.design.cupertinoDarkScheme
import com.scottolcott.recipe.ui.design.cupertinoLightScheme
import com.scottolcott.recipe.ui.design.cupertinoShapes
import com.scottolcott.recipe.ui.design.cupertinoTypography
import com.scottolcott.recipe.ui.design.pointerShapes
import com.scottolcott.recipe.ui.design.pointerTypography

private val lightScheme =
  lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
  )

private val darkScheme =
  darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
  )

/**
 * The app's theme, in whichever design language the platform calls for.
 *
 * `MaterialTheme` stays the substrate under every design. Only what it hands out changes, which is
 * what lets every `ListItem`, `Text`, `AssistChip` and `IconButton` already in the app pick up iOS
 * colours, metrics and corner radii -- or a pointer's tighter ones -- without being touched, and
 * what keeps the Cupertino work from needing a parallel component set.
 *
 * [design] and [input] arrive as parameters rather than being read from their locals so this stays
 * usable on its own in previews. It provides both locals from the same values, so the tokens and
 * the `isCupertino`/`isPointer` branches below can never disagree about which design is in force.
 */
@Composable
fun RecipeAppTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  design: AppDesign = if (isIos()) AppDesign.Cupertino else AppDesign.Material,
  input: AppInput = if (isDesktop() || isWeb()) AppInput.Pointer else AppInput.Touch,
  content: @Composable () -> Unit,
) {
  val cupertino = design == AppDesign.Cupertino
  val pointer = input == AppInput.Pointer

  MaterialTheme(
    colorScheme = colorSchemeFor(cupertino, darkTheme),
    typography = typographyFor(cupertino, pointer),
    shapes = shapesFor(cupertino, pointer),
  ) {
    val indication =
      if (cupertino) remember { CupertinoFadeIndication() } else LocalIndication.current
    CompositionLocalProvider(
      LocalAppDesign provides design,
      LocalAppInput provides input,
      LocalIndication provides indication,
      // Material's 48dp minimum is sized for a fingertip. A mouse hits a far smaller target, and
      // the padding it forces around every icon button is most of what makes the app read as a
      // phone build stretched wide. 36dp keeps a comfortable margin over the 24px floor WCAG 2.2
      // sets for pointer input -- do not chase density below that.
      LocalMinimumInteractiveComponentSize provides
        if (pointer) POINTER_MIN_INTERACTIVE_SIZE else TOUCH_MIN_INTERACTIVE_SIZE,
      content = content,
    )
  }
}

/**
 * Colour is the one token set the input does not fork.
 *
 * Density, type and shape carry the difference between touch and pointer; giving the desktop its
 * own palette would say the app is a different product there, which it is not.
 */
private fun colorSchemeFor(cupertino: Boolean, darkTheme: Boolean) =
  when {
    cupertino && darkTheme -> cupertinoDarkScheme
    cupertino -> cupertinoLightScheme
    darkTheme -> darkScheme
    else -> lightScheme
  }

private fun typographyFor(cupertino: Boolean, pointer: Boolean) =
  when {
    cupertino -> cupertinoTypography
    pointer -> pointerTypography
    else -> AppTypography
  }

/**
 * `MaterialTheme.shapes` is read outside the [MaterialTheme] this file installs, so it resolves to
 * Material's own defaults -- the Material design's answer, named rather than restated.
 */
@Composable
@ReadOnlyComposable
private fun shapesFor(cupertino: Boolean, pointer: Boolean) =
  when {
    cupertino -> cupertinoShapes
    pointer -> pointerShapes
    else -> MaterialTheme.shapes
  }

/** Material's own default, restated so the pair reads as a choice rather than an override. */
private val TOUCH_MIN_INTERACTIVE_SIZE = 48.dp
private val POINTER_MIN_INTERACTIVE_SIZE = 36.dp
