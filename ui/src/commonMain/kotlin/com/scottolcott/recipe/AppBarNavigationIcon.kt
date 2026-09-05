package com.scottolcott.recipe

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

/**
 * The leading icon the search bar should draw when it stands in for the top app bar.
 *
 * On a layout wide enough to keep search up permanently, `AppBarWithSearch` *is* the top app bar --
 * `RecipeAppBar` renders it instead of `TopAppBar`, not beside it. Back is the only chrome that has
 * to travel with it: the navigation rail beside it already carries the app mark and Favorites, and
 * the search action would point at the field itself.
 *
 * A composition local rather than a parameter because the search bar is a sub-circuit:
 * `RecipeAppBar` reaches it through `SubCircuitContent(SearchScreen(...))`, whose only entry point
 * is `SubUi.Content(state, modifier)`. There is no slot to pass a composable through, and a
 * composable cannot ride inside the `Screen` -- that is a serializable data class.
 *
 * `null` leaves the slot out entirely, which is what web wants: the browser's own back button makes
 * an in-app one redundant, and an empty-but-present slot would still cost Material3's leading
 * padding.
 *
 * Deliberately not `staticCompositionLocalOf`: the value is a lambda closing over the scaffold
 * state, so its identity changes whenever that state does. A static local would take the whole
 * search sub-circuit -- suggestion list included -- down with it on every one of those changes,
 * where this one recomposes only the bar that actually reads it.
 */
val LocalAppBarNavigationIcon = compositionLocalOf<(@Composable () -> Unit)?> { null }
