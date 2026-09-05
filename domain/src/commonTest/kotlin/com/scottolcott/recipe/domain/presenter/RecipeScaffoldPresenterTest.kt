package com.scottolcott.recipe.domain.presenter

import com.scottolcott.recipe.model.RecipeId
import com.slack.circuit.runtime.screen.Screen
import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The scaffold's navigation-rail behaviour, covered through the pure helpers it is built from.
 *
 * `RecipeScaffoldPresenter.present` builds its own `NavStack` and reads `LocalDeepLinkScreen` and
 * `LocalWindowSizeClass`, so exercising the rail through the presenter would mean standing up a
 * composition with both locals provided. The decisions worth pinning down -- which screens count as
 * the same destination, and which destination a stack position sits under -- live in the helpers.
 */
private val details = RecipeDetailsScreen(RecipeId("52772"))

val recipeScaffoldPresenterTests by testSuite {
  test("a home screen matches home whatever tab it carries") {
    assertTrue(HomeScreen().isSameDestinationAs(HomeScreen()))
    assertTrue(HomeScreen(AreasScreen).isSameDestinationAs(HomeScreen()))
    assertTrue(HomeScreen().isSameDestinationAs(HomeScreen(IngredientsScreen)))
  }

  test("other destinations still compare by equality") {
    assertTrue(RecipesScreen.Favorites.isSameDestinationAs(RecipesScreen.Favorites))
    assertFalse(RecipesScreen.Favorites.isSameDestinationAs(HomeScreen()))
    assertFalse(HomeScreen().isSameDestinationAs(RecipesScreen.Favorites))
    assertFalse(RecipesScreen.ByCategory("Seafood").isSameDestinationAs(RecipesScreen.Favorites))
    assertFalse(
      RecipesScreen.ByCategory("Seafood").isSameDestinationAs(RecipesScreen.ByCategory("Beef"))
    )
  }

  val selections =
    listOf(
      Triple("the root alone", listOf<Screen>(HomeScreen()), HomeScreen()),
      Triple("a screen pushed from home", listOf(details, HomeScreen()), HomeScreen()),
      Triple(
        "a screen pushed from favorites",
        listOf(details, RecipesScreen.Favorites, HomeScreen()),
        RecipesScreen.Favorites,
      ),
      Triple(
        "a deep-linked home tab",
        listOf(HomeScreen(AreasScreen)),
        // The rail's own HomeScreen(), not the record's -- the tab is not part of the destination.
        HomeScreen(),
      ),
      Triple("nothing at all", emptyList(), null),
    )

  for ((name, screens, expected) in selections) {
    test("selected destination for $name") { assertEquals(expected, screens.selectedDestination()) }
  }

  // `savedRootFor` looks a destination up among `NavStack.peekState()`'s keys, which are whole root
  // screens rather than destinations -- a deep-linked Home is filed under the tab it was seeded
  // with, while the rail asks for a plain HomeScreen().
  test("a saved stack is found under the root screen it was filed as") {
    val savedRoots = listOf<Screen>(HomeScreen(AreasScreen), RecipesScreen.Favorites)
    assertEquals(
      HomeScreen(AreasScreen),
      savedRoots.firstOrNull { it.isSameDestinationAs(HomeScreen()) },
    )
    assertEquals(
      RecipesScreen.Favorites,
      savedRoots.firstOrNull { it.isSameDestinationAs(RecipesScreen.Favorites) },
    )
    assertEquals(null, savedRoots.firstOrNull { it.isSameDestinationAs(details) })
  }
}
