package com.scottolcott.recipe.model

import kotlinx.serialization.Serializable

// TODO move to different module

/**
 * Represents a unique identifier for a set of recipes fetched from a specific source.
 *
 * This key is used to distinguish between different types of recipe data requests (e.g., searching
 * by name, category, or area) and is used to track the network fetch history for each request to
 * determine when data needs to be refreshed.
 */
@Serializable
sealed interface RecipeKey {
  /** Key for recipes fetched via a search query. */
  @Serializable data class Query(val query: String) : RecipeKey

  /** Key for a specific recipe fetched by its unique ID. */
  @Serializable data class ById(val id: RecipeId) : RecipeKey

  /** Key for recipes belonging to a specific category. */
  @Serializable data class ByCategory(val category: String) : RecipeKey

  /** Key for recipes originating from a specific geographic area. */
  @Serializable data class ByArea(val area: String) : RecipeKey

  /** Key for the user's favorite recipes, which are stored locally. */
  @Serializable data object Favorites : RecipeKey
}
