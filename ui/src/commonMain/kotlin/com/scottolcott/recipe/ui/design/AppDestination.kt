package com.scottolcott.recipe.ui.design

import androidx.compose.runtime.Immutable
import com.slack.circuit.runtime.screen.Screen
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

/**
 * One top-level section, as both the navigation rail and the tab bar need to draw it.
 *
 * Shared rather than declared twice because the two surfaces differ only in layout: the same
 * screen, icon pair and label drive both, so keeping one list means adding a section cannot leave
 * the two disagreeing.
 */
@Immutable
data class AppDestination(
  val screen: Screen,
  val icon: DrawableResource,
  val selectedIcon: DrawableResource,
  val label: StringResource,
) {
  fun iconFor(selected: Boolean): DrawableResource = if (selected) selectedIcon else icon

  /**
   * Whether [current] sits under this destination.
   *
   * Plain equality is enough despite `HomeScreen` being a data class carrying its selected tab:
   * `RecipeScaffoldState.selectedDestination` is resolved by the presenter against
   * `TOP_LEVEL_DESTINATIONS` and hands back the entry from that list, not the screen actually on
   * the stack. So a recipe opened from Favorites arrives here as `RecipesScreen.Favorites`, and
   * `HomeScreen(AreasScreen)` arrives as the canonical `HomeScreen()`.
   */
  fun isSelectedBy(current: Screen?): Boolean = current == screen
}
