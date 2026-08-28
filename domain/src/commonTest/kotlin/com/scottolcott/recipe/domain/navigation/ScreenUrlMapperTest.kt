package com.scottolcott.recipe.domain.navigation

import com.scottolcott.recipe.domain.presenter.CategoriesScreen
import com.scottolcott.recipe.domain.presenter.RecipeDetailsScreen
import com.scottolcott.recipe.domain.presenter.RecipeScaffoldScreen
import com.scottolcott.recipe.domain.presenter.RecipesScreen
import com.scottolcott.recipe.model.RecipeId
import com.slack.circuit.runtime.screen.Screen
import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals
import kotlin.test.assertNull

private val roundTripScreens: List<Pair<Screen, String>> =
  listOf(
    CategoriesScreen to "/home",
    RecipesScreen.ByCategory("Seafood") to "/recipes/category/Seafood",
    RecipesScreen.ByArea("British") to "/recipes/area/British",
    RecipesScreen.BySearch("chicken") to "/recipes/search/chicken",
    RecipesScreen.Favorites to "/recipes/favorites",
    RecipeDetailsScreen(RecipeId("52772")) to "/recipe/52772",
  )

val screenUrlMapperTests by testSuite {
  for ((screen, path) in roundTripScreens) {
    test("toUrlPath $path") { assertEquals(path, screen.toUrlPath()) }

    test("urlPathToScreen $path") { assertEquals(screen, urlPathToScreen(path)) }
  }

  test("internal screens have no url") { assertNull(RecipeScaffoldScreen.toUrlPath()) }

  test("deep links drop the scheme and authority") {
    assertEquals(
      RecipeDetailsScreen(RecipeId("52772")),
      urlPathToScreen("recipes://app/recipe/52772"),
    )
  }

  test("deep link without a path is home") {
    assertEquals(CategoriesScreen, urlPathToScreen("recipes://app"))
  }

  test("empty and root paths are home") {
    assertEquals(CategoriesScreen, urlPathToScreen(""))
    assertEquals(CategoriesScreen, urlPathToScreen("/"))
  }

  test("trailing slashes are ignored") {
    assertEquals(RecipesScreen.Favorites, urlPathToScreen("/recipes/favorites/"))
  }

  test("encoded segments round trip") {
    val screen = RecipesScreen.BySearch("chicken soup")
    assertEquals("/recipes/search/chicken%20soup", screen.toUrlPath())
    assertEquals(screen, urlPathToScreen("/recipes/search/chicken%20soup"))
  }

  test("blank segments are unrecognised") {
    assertNull(urlPathToScreen("/recipes/category/"))
    assertNull(urlPathToScreen("/recipe/"))
  }

  test("unknown paths are unrecognised") {
    assertNull(urlPathToScreen("/nope"))
    assertNull(urlPathToScreen("recipes://app/nope"))
  }
}
