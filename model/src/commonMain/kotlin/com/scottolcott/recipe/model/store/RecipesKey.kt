package com.scottolcott.recipe.model.store

import com.scottolcott.recipe.model.RecipeId
import com.scottolcott.recipe.model.normalizeIngredient
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

  /**
   * Key for recipes containing *all* of [ingredients].
   *
   * Build one through [ByIngredient.of] rather than the constructor, which is private: `of`
   * normalizes and sorts the names so the key has a single canonical form. That matters because the
   * key is a map key in the persisted fetch history, so `["Beef"]` and `["beef"]` must not become
   * two different cache entries — and because the same normalization backs the indexed lookup
   * column in storage.
   */
  @ConsistentCopyVisibility
  @Serializable
  data class ByIngredient private constructor(val ingredients: List<String>) : RecipesKey {
    companion object {
      fun of(ingredients: Collection<String>): ByIngredient =
        ByIngredient(
          ingredients
            .map { it.normalizeIngredient() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
        )

      fun of(vararg ingredients: String): ByIngredient = of(ingredients.asList())
    }
  }

  /** Key for recipes originating from a specific geographic area. */
  @Serializable data class ByArea(val area: String) : RecipesKey

  /** Key for the user's favorite recipes, which are stored locally. */
  @Serializable data object Favorites : RecipesKey
}
