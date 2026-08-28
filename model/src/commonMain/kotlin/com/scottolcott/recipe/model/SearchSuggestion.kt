package com.scottolcott.recipe.model

import kotlinx.serialization.Serializable

@Serializable
sealed interface SearchSuggestion {
  @Serializable data class QuerySuggestion(val query: String) : SearchSuggestion

  @Serializable data class CategorySuggestion(val category: Category) : SearchSuggestion

  @Serializable data class IngredientSuggestion(val ingredient: Ingredient) : SearchSuggestion
}
