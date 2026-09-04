package com.scottolcott.recipe.domain.presenter

import com.scottolcott.recipe.model.RecipeId
import com.slack.circuit.runtime.screen.CircuitSaveable
import com.slack.circuit.runtime.screen.Screen
import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * Every [Screen] is persisted by `SerializableCircuitSaver`, which encodes it against a polymorphic
 * `CircuitSaveable` serializer. Under `@Parcelize` a screen that could not be persisted failed to
 * compile; under `@CircuitSerializable` it fails at save time instead, so the round trip is checked
 * here.
 *
 * This mirrors the module Circuit codegen builds from `@CircuitSerializable`, rather than reading
 * the multibound registrations, which would need the whole `AppScope` graph. Whether a screen is
 * registered stays a compile-time concern; what this covers is that its serializer exists and that
 * every property survives the trip.
 */
private val screenSerializers = SerializersModule {
  polymorphic(CircuitSaveable::class) {
    subclass(HomeScreen::class, HomeScreen.serializer())
    subclass(CategoriesScreen::class, CategoriesScreen.serializer())
    subclass(AreasScreen::class, AreasScreen.serializer())
    subclass(IngredientsScreen::class, IngredientsScreen.serializer())
    subclass(RecipeDetailsScreen::class, RecipeDetailsScreen.serializer())
    subclass(RecipeScaffoldScreen::class, RecipeScaffoldScreen.serializer())
    subclass(RecipesScreen.ByArea::class, RecipesScreen.ByArea.serializer())
    subclass(RecipesScreen.ByCategory::class, RecipesScreen.ByCategory.serializer())
    subclass(RecipesScreen.BySearch::class, RecipesScreen.BySearch.serializer())
    subclass(RecipesScreen.ByIngredient::class, RecipesScreen.ByIngredient.serializer())
    subclass(RecipesScreen.Favorites::class, RecipesScreen.Favorites.serializer())
  }
}

private val json = Json { serializersModule = screenSerializers }

private val roundTripScreens: List<Screen> =
  listOf(
    HomeScreen(),
    HomeScreen(AreasScreen),
    CategoriesScreen,
    AreasScreen,
    IngredientsScreen,
    RecipeScaffoldScreen,
    RecipesScreen.ByCategory("Seafood"),
    RecipesScreen.ByArea("British"),
    RecipesScreen.BySearch("chicken soup"),
    RecipesScreen.ByIngredient(setOf("chicken", "rice")),
    RecipesScreen.Favorites,
    RecipeDetailsScreen(RecipeId("52772")),
  )

val screenSerializationTests by testSuite {
  for (screen in roundTripScreens) {
    test("$screen round trips") {
      val serializer = PolymorphicSerializer(CircuitSaveable::class)
      val encoded = json.encodeToString(serializer, screen)
      assertEquals(screen, json.decodeFromString(serializer, encoded))
    }
  }

  // HomePresenter persists the selected tab through HomeTabScreen.serializer() -- the closed sealed
  // serializer -- not through the CircuitSaveable module above. Adding @Polymorphic to
  // HomeTabScreen would switch this to open polymorphism, which nothing registers a module for,
  // so cover it separately.
  for (tab in listOf(CategoriesScreen, IngredientsScreen, AreasScreen)) {
    test("${tab::class.simpleName} round trips as a HomeTabScreen") {
      val serializer = HomeTabScreen.serializer()
      val encoded = Json.encodeToString(serializer, tab)
      assertEquals(tab, Json.decodeFromString(serializer, encoded))
    }
  }
}
