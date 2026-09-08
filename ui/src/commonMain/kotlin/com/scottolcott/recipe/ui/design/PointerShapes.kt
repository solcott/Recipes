package com.scottolcott.recipe.ui.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material's radii, pulled in for a pointer.
 *
 * Material's ramp (4/8/12/16/28) is drawn for controls sized to a fingertip; on a card that is no
 * longer 48dp tall the same radius eats a visible share of the corner and reads as chrome rather
 * than as a container. Each step comes back roughly a third, and `extraSmall` stays at 4dp because
 * it is already at the floor where a radius still reads as intentional.
 *
 * This is one of the two token sets that carry the pointer design, alongside [pointerTypography].
 * Colours are deliberately *not* forked -- see `RecipeAppTheme`.
 */
internal val pointerShapes =
  Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(20.dp),
  )
