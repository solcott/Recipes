package com.scottolcott.recipe.ui.design

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.compositionLocalOf

/**
 * The scroll behaviour the Cupertino large title collapses against, or `null` under Material.
 *
 * A composition local rather than a parameter because the bar and the thing that scrolls it are far
 * apart: the bar is built in `RecipeAppBar`, while the scroll arrives from whichever screen
 * `NavigableCircuitContent` happens to be showing. Threading it would mean a parameter on every
 * screen signature for something only one design uses.
 *
 * Note the scroll itself does *not* travel this way -- `Modifier.nestedScroll` on the scaffold
 * catches it from any descendant automatically. This local exists only so the bar can find the same
 * behaviour object, and so the scaffold can reset it when the screen changes.
 *
 * Deliberately not `staticCompositionLocalOf`: the value changes when the design does, and readers
 * should recompose with it.
 */
@OptIn(ExperimentalMaterial3Api::class)
val LocalTopAppBarScrollBehavior = compositionLocalOf<TopAppBarScrollBehavior?> { null }
