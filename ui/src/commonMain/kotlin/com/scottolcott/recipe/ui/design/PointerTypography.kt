package com.scottolcott.recipe.ui.design

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.scottolcott.recipe.ui.theme.AppTypography

/**
 * Material's type ramp, stepped down one notch for a display you sit back from.
 *
 * Material sizes its scale for a phone held at arm's length, where 16sp body is right. A desktop or
 * browser window is further away but far larger, and every system it sits next to sets its own body
 * text smaller -- 13pt on macOS, 12pt on Windows. Left at Material's sizes the app reads as a phone
 * build stretched wide, which is the thing this design exists to fix.
 *
 * Only `title`, `body` and `label` move. `display` and `headline` are accents rather than density:
 * a large title costs one line of the window and is the anchor the eye returns to, so shrinking it
 * would spend the change where there is nothing to win.
 *
 * Each slot is derived from the Material one rather than written out, so weight, font family and
 * every property this does not name stay exactly as Material set them -- and the `*Emphasized`
 * slots move with their base slot rather than being left a size behind. Four of them are live here
 * (`titleMediumEmphasized` and `titleSmallEmphasized` in the category labels,
 * `headlineMediumEmphasized` in the recipe detail title, `labelSmallEmphasized` in the tab labels),
 * and the first two would otherwise sit a notch above the `titleMedium` they share a screen with.
 *
 * Tracking comes in with the size: Material's positive tracking buys legibility at small sizes on a
 * phone, and holding it while the size drops leaves body text looking loose.
 */
private fun TextStyle.stepped(size: Int, lineHeight: Int, tracking: Float) =
  copy(fontSize = size.sp, lineHeight = lineHeight.sp, letterSpacing = tracking.sp)

internal val pointerTypography =
  Typography(
    // 22/28 -> 20/26.
    titleLarge = AppTypography.titleLarge.stepped(20, 26, 0f),
    // 16/24 -> 15/22.
    titleMedium = AppTypography.titleMedium.stepped(15, 22, 0.1f),
    // 14/20 -> 13/18.
    titleSmall = AppTypography.titleSmall.stepped(13, 18, 0.1f),
    // 16/24 -> 14/21. The body step is the one that decides how much of a list fits in a window.
    bodyLarge = AppTypography.bodyLarge.stepped(14, 21, 0.25f),
    // 14/20 -> 13/19.
    bodyMedium = AppTypography.bodyMedium.stepped(13, 19, 0.1f),
    // 14/20 -> 13/18. Buttons and the tab labels ride on this one.
    labelLarge = AppTypography.labelLarge.stepped(13, 18, 0.1f),
    // 12/16 -> 11/15.
    labelMedium = AppTypography.labelMedium.stepped(11, 15, 0.4f),
    titleLargeEmphasized = AppTypography.titleLargeEmphasized.stepped(20, 26, 0f),
    titleMediumEmphasized = AppTypography.titleMediumEmphasized.stepped(15, 22, 0.1f),
    titleSmallEmphasized = AppTypography.titleSmallEmphasized.stepped(13, 18, 0.1f),
    bodyLargeEmphasized = AppTypography.bodyLargeEmphasized.stepped(14, 21, 0.25f),
    bodyMediumEmphasized = AppTypography.bodyMediumEmphasized.stepped(13, 19, 0.1f),
    labelLargeEmphasized = AppTypography.labelLargeEmphasized.stepped(13, 18, 0.1f),
    labelMediumEmphasized = AppTypography.labelMediumEmphasized.stepped(11, 15, 0.4f),
  )
