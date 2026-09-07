package com.scottolcott.recipe.ui.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Apple's corner radii.
 *
 * Flatter than Material's ramp throughout -- iOS reserves large radii for cards and sheets and
 * keeps controls comparatively square. `medium` is 10dp because that is the inset-grouped table
 * radius, which every card in the app inherits through [AppCard].
 */
internal val cupertinoShapes =
  Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
  )
