package com.scottolcott.recipe.model.store

import com.scottolcott.recipe.model.RecipeId
import kotlinx.serialization.Serializable

/**
 * Represents a unique identifier for a set of recipes fetched from a specific source.
 *
 * This key is used to distinguish between different types of recipe data requests (e.g., searching
 * by name, category, or area) and is used to track the network fetch history for each request to
 * determine when data needs to be refreshed.
 */
@Serializable
sealed interface RecipesKey {
  /** Key for recipes fetched via a search query. */
  @Serializable data class Query(val query: String) : RecipesKey

  /** Key for a specific recipe fetched by its unique ID. */
  @Serializable data class ById(val id: RecipeId) : RecipesKey

  /** Key for recipes belonging to a specific category. */
  @Serializable data class ByCategory(val category: String) : RecipesKey

  /** Key for recipes originating from a specific geographic area. */
  @Serializable data class ByArea(val area: String) : RecipesKey

  /** Key for the user's favorite recipes, which are stored locally. */
  @Serializable data object Favorites : RecipesKey
}
