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
    RecipesScreen.ByIngredient(setOf("chicken")) to "/recipes/ingredient/chicken",
    RecipesScreen.ByIngredient(setOf("rice", "chicken")) to "/recipes/ingredient/chicken,rice",
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
    assertNull(urlPathToScreen("/recipes/ingredient/"))
    assertNull(urlPathToScreen("/recipes/ingredient/,,"))
  }

  test("ingredient order does not affect the url") {
    assertEquals(
      RecipesScreen.ByIngredient(setOf("chicken", "rice")).toUrlPath(),
      RecipesScreen.ByIngredient(setOf("rice", "chicken")).toUrlPath(),
    )
  }

  // encodeURLPathPart leaves ',' alone because it is a legal path character, which would let a name
  // containing one split into two on the way back. The ingredient segment encodes more
  // aggressively.
  test("an ingredient containing the separator survives the round trip") {
    val screen = RecipesScreen.ByIngredient(setOf("salt, coarse", "chicken breast"))
    assertEquals("/recipes/ingredient/chicken%20breast,salt%2C%20coarse", screen.toUrlPath())
    assertEquals(screen, urlPathToScreen(screen.toUrlPath()!!))
  }

  test("duplicate ingredients collapse") {
    assertEquals(
      RecipesScreen.ByIngredient(setOf("chicken")),
      urlPathToScreen("/recipes/ingredient/chicken,chicken"),
    )
  }

  test("unknown paths are unrecognised") {
    assertNull(urlPathToScreen("/nope"))
    assertNull(urlPathToScreen("recipes://app/nope"))
  }
}
