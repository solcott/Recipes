package com.scottolcott.recipe.ui.design

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Apple's system colors, mapped onto Material 3's roles.
 *
 * The mapping is the whole trick behind this design system: by keeping `MaterialTheme` as the
 * substrate and only swapping what it hands out, every `ListItem`, `Text`, `AssistChip` and
 * `IconButton` already in the app picks up iOS colors without being touched. Only chrome with no
 * Material counterpart -- the tab bar, the large title, grouped sections -- needs new composables.
 *
 * Two role choices are worth spelling out, because they invert the Material habit:
 * - `background` is the *grouped* background (the grey page), while `surface` is the raised row
 *   colour (white). On iOS the page is darker than the cards sitting on it, which is the opposite
 *   of Material's elevation model.
 * - `outlineVariant` is the opaque separator grey, not a tinted outline, so hairlines read as iOS
 *   separators rather than as borders.
 *
 * Apple specifies several of these with alpha over an unknown backdrop. Where that would force
 * every consumer to composite correctly, the opaque equivalent over the standard background is used
 * instead.
 */
private object SystemColors {
  // Light
  val blueLight = Color(0xFF007AFF)
  // Blue at 15% over white. Pre-flattened rather than composited at use, and deliberately distinct
  // from every other role -- see the note on collisions above.
  val blueTintLight = Color(0xFFD9EBFF)
  val labelLight = Color(0xFF000000)
  val secondaryLabelLight = Color(0xFF8A8A8E)
  val groupedBackgroundLight = Color(0xFFF2F2F7)
  val groupedSurfaceLight = Color(0xFFFFFFFF)
  val separatorLight = Color(0xFFC6C6C8)
  val fillLight = Color(0xFFE3E3E8)
  val redLight = Color(0xFFFF3B30)

  // Dark
  val blueDark = Color(0xFF0A84FF)
  /** Blue at 20% over [groupedSurfaceDark]. */
  val blueTintDark = Color(0xFF18314B)
  val labelDark = Color(0xFFFFFFFF)
  val secondaryLabelDark = Color(0xFF8E8E93)
  val groupedBackgroundDark = Color(0xFF000000)
  val groupedSurfaceDark = Color(0xFF1C1C1E)
  val separatorDark = Color(0xFF38383A)
  val fillDark = Color(0xFF2C2C2E)
  val redDark = Color(0xFFFF453A)
}

internal val cupertinoLightScheme: ColorScheme =
  lightColorScheme(
    primary = SystemColors.blueLight,
    onPrimary = Color.White,
    primaryContainer = SystemColors.blueTintLight,
    onPrimaryContainer = SystemColors.blueLight,
    secondary = SystemColors.blueLight,
    onSecondary = Color.White,
    secondaryContainer = SystemColors.fillLight,
    onSecondaryContainer = SystemColors.labelLight,
    tertiary = SystemColors.blueLight,
    onTertiary = Color.White,
    tertiaryContainer = SystemColors.fillLight,
    onTertiaryContainer = SystemColors.labelLight,
    error = SystemColors.redLight,
    onError = Color.White,
    background = SystemColors.groupedBackgroundLight,
    onBackground = SystemColors.labelLight,
    surface = SystemColors.groupedSurfaceLight,
    onSurface = SystemColors.labelLight,
    surfaceVariant = SystemColors.fillLight,
    onSurfaceVariant = SystemColors.secondaryLabelLight,
    outline = SystemColors.secondaryLabelLight,
    outlineVariant = SystemColors.separatorLight,
    surfaceContainerLowest = SystemColors.groupedSurfaceLight,
    surfaceContainerLow = SystemColors.groupedSurfaceLight,
    surfaceContainer = SystemColors.groupedBackgroundLight,
    surfaceContainerHigh = SystemColors.fillLight,
    surfaceContainerHighest = SystemColors.fillLight,
  )

internal val cupertinoDarkScheme: ColorScheme =
  darkColorScheme(
    primary = SystemColors.blueDark,
    onPrimary = Color.White,
    primaryContainer = SystemColors.blueTintDark,
    onPrimaryContainer = SystemColors.blueDark,
    secondary = SystemColors.blueDark,
    onSecondary = Color.White,
    secondaryContainer = SystemColors.fillDark,
    onSecondaryContainer = SystemColors.labelDark,
    tertiary = SystemColors.blueDark,
    onTertiary = Color.White,
    tertiaryContainer = SystemColors.fillDark,
    onTertiaryContainer = SystemColors.labelDark,
    error = SystemColors.redDark,
    onError = Color.White,
    background = SystemColors.groupedBackgroundDark,
    onBackground = SystemColors.labelDark,
    surface = SystemColors.groupedSurfaceDark,
    onSurface = SystemColors.labelDark,
    surfaceVariant = SystemColors.fillDark,
    onSurfaceVariant = SystemColors.secondaryLabelDark,
    outline = SystemColors.secondaryLabelDark,
    outlineVariant = SystemColors.separatorDark,
    surfaceContainerLowest = SystemColors.groupedBackgroundDark,
    surfaceContainerLow = SystemColors.groupedSurfaceDark,
    surfaceContainer = SystemColors.groupedSurfaceDark,
    surfaceContainerHigh = SystemColors.fillDark,
    surfaceContainerHighest = SystemColors.fillDark,
  )
