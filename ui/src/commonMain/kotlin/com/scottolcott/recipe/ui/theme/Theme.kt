package com.scottolcott.recipe.ui.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.scottolcott.recipe.domain.AppDesign
import com.scottolcott.recipe.isIos
import com.scottolcott.recipe.ui.design.CupertinoFadeIndication
import com.scottolcott.recipe.ui.design.cupertinoDarkScheme
import com.scottolcott.recipe.ui.design.cupertinoLightScheme
import com.scottolcott.recipe.ui.design.cupertinoShapes
import com.scottolcott.recipe.ui.design.cupertinoTypography

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
 * `MaterialTheme` stays the substrate under both designs. Only what it hands out changes, which is
 * what lets every `ListItem`, `Text`, `AssistChip` and `IconButton` already in the app pick up iOS
 * colours, metrics and corner radii without being touched -- and what keeps the Cupertino work from
 * needing a parallel component set.
 *
 * [design] arrives as a parameter rather than being read from `LocalAppDesign` so this stays usable
 * on its own in previews. `RecipeApp` provides the local from the same value, above this.
 */
@Composable
fun RecipeAppTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  design: AppDesign = if (isIos()) AppDesign.Cupertino else AppDesign.Material,
  content: @Composable () -> Unit,
) {
  val cupertino = design == AppDesign.Cupertino
  val colorScheme =
    when {
      cupertino && darkTheme -> cupertinoDarkScheme
      cupertino -> cupertinoLightScheme
      darkTheme -> darkScheme
      else -> lightScheme
    }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = if (cupertino) cupertinoTypography else AppTypography,
    shapes = if (cupertino) cupertinoShapes else MaterialTheme.shapes,
  ) {
    val indication =
      if (cupertino) remember { CupertinoFadeIndication() } else LocalIndication.current
    CompositionLocalProvider(LocalIndication provides indication, content = content)
  }
}
