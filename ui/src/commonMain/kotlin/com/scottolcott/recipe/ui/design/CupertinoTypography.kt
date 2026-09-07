package com.scottolcott.recipe.ui.design

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Apple's type ramp, mapped onto Material 3's slots.
 *
 * No font is bundled and none is needed: `FontFamily.Default` already resolves to the system face,
 * which is SF Pro on iOS. Only the metrics have to change.
 *
 * The tracking values are Apple's own per-size table, which is not monotonic -- display sizes get
 * slightly positive tracking while 17pt body gets `-0.41`. Copying the table rather than
 * interpolating is what keeps body text from looking subtly too wide.
 *
 * Two slots are set for a component rather than for their Material meaning, and both are safe
 * because nothing in this app reads them directly: `headlineMedium` is what `LargeTopAppBar` uses
 * for its expanded title and `titleLarge` is what it uses for the collapsed one, so mapping them to
 * Apple's large-title and headline styles is what makes the bar collapse 34pt bold -> 17pt semibold
 * without a hand-rolled app bar.
 *
 * Material's `*Emphasized` slots have no iOS counterpart, so they map to the semibold cut of the
 * same size. Three of them are live in this app (`headlineMediumEmphasized` in the recipe detail
 * title, `titleMediumEmphasized` and `titleSmallEmphasized` in the category labels); leaving them
 * at Material defaults would have left those exact strings in Material metrics.
 */
private fun sf(
  size: Int,
  lineHeight: Int,
  weight: FontWeight = FontWeight.Normal,
  tracking: Float = 0f,
) =
  TextStyle(
    fontFamily = FontFamily.Default,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    fontWeight = weight,
    letterSpacing = tracking.sp,
  )

// Apple's named styles, kept under their own names so the mapping below stays readable.
private val largeTitle = sf(34, 41, FontWeight.Bold, 0.37f)
private val title1 = sf(28, 34, FontWeight.Normal, 0.36f)
private val title1Emphasized = sf(28, 34, FontWeight.Bold, 0.36f)
private val title2 = sf(22, 28, FontWeight.Normal, 0.35f)
private val title2Emphasized = sf(22, 28, FontWeight.Bold, 0.35f)
private val title3Emphasized = sf(20, 25, FontWeight.SemiBold, 0.38f)
private val headline = sf(17, 22, FontWeight.SemiBold, -0.41f)
private val body = sf(17, 22, FontWeight.Normal, -0.41f)
private val callout = sf(16, 21, FontWeight.Normal, -0.32f)
private val subhead = sf(15, 20, FontWeight.Normal, -0.24f)
private val subheadEmphasized = sf(15, 20, FontWeight.SemiBold, -0.24f)
private val footnote = sf(13, 18, FontWeight.Normal, -0.08f)
private val footnoteEmphasized = sf(13, 18, FontWeight.SemiBold, -0.08f)
private val caption1 = sf(12, 16)
private val caption1Emphasized = sf(12, 16, FontWeight.Medium)
private val caption2 = sf(11, 13, FontWeight.Normal, 0.07f)
private val caption2Emphasized = sf(11, 13, FontWeight.Medium, 0.07f)

internal val cupertinoTypography =
  Typography(
    displayLarge = largeTitle,
    displayMedium = title1,
    displaySmall = title2,
    headlineLarge = largeTitle,
    headlineMedium = largeTitle,
    headlineSmall = title2,
    titleLarge = headline,
    titleMedium = headline,
    titleSmall = subhead,
    bodyLarge = body,
    bodyMedium = callout,
    bodySmall = footnote,
    labelLarge = headline,
    labelMedium = caption1,
    labelSmall = caption2,
    displayLargeEmphasized = largeTitle,
    displayMediumEmphasized = title1Emphasized,
    displaySmallEmphasized = title2Emphasized,
    headlineLargeEmphasized = largeTitle,
    headlineMediumEmphasized = title1Emphasized,
    headlineSmallEmphasized = title2Emphasized,
    titleLargeEmphasized = title3Emphasized,
    titleMediumEmphasized = headline,
    titleSmallEmphasized = subheadEmphasized,
    bodyLargeEmphasized = headline,
    bodyMediumEmphasized = sf(16, 21, FontWeight.SemiBold, -0.32f),
    bodySmallEmphasized = footnoteEmphasized,
    labelLargeEmphasized = headline,
    labelMediumEmphasized = caption1Emphasized,
    labelSmallEmphasized = caption2Emphasized,
  )
