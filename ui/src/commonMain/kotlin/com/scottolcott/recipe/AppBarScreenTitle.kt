package com.scottolcott.recipe

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import com.scottolcott.recipe.domain.isCupertino
import com.scottolcott.recipe.domain.presenter.RecipeScaffoldState

/**
 * Whether the top app bar is showing the current screen's name.
 *
 * A screen reads this to decide whether to head itself. Under Cupertino the bar usually carries the
 * name, so repeating it in the content would show it twice -- but on a layout wide enough for the
 * navigation rail the search field takes the bar over entirely, and a screen that stands its
 * heading down there is named nowhere at all. That is what left an iPad nameless on every list.
 *
 * A composition local for the same reason as [LocalAppBarNavigationIcon]: the bar is built in
 * `RecipeAppBar` and the readers are whichever screen `NavigableCircuitContent` is showing, so
 * threading it would mean a parameter on every screen signature. `RecipeScaffoldScreen` provides
 * it; see `appBarShowsScreenTitle`.
 */
val LocalAppBarShowsScreenTitle = compositionLocalOf { false }

/**
 * Whether the top app bar is currently carrying the name of the screen.
 *
 * Cupertino puts that name in the bar -- see `navigationBarTitle` -- but only while the bar is
 * still a bar: on a layout wide enough for the navigation rail the search field takes it over
 * whole, and then nothing up there names anything. Screens read this through
 * [LocalAppBarShowsScreenTitle] to decide whether to head themselves.
 */
@Composable
internal fun RecipeScaffoldState.appBarShowsScreenTitle(): Boolean = isCupertino && !searchVisible

/**
 * Whether that name is the large title that collapses as the user scrolls, rather than the inline
 * one over a back chevron.
 *
 * The `when` that picks a bar in `RecipeAppBar` is one reader; `RecipeScaffoldScreen` is the other,
 * and it is why this is not left inline there: the collapse's nested-scroll connection must be
 * attached only while the bar that consumes it is on screen.
 */
@Composable
internal fun RecipeScaffoldState.appBarTitleCollapses(): Boolean =
  appBarShowsScreenTitle() && !showBackButton(this)
