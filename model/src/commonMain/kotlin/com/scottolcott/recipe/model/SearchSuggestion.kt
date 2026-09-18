package com.scottolcott.recipe.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Serializable
@Immutable
sealed interface SearchSuggestion {
  @Serializable data class QuerySuggestion(val query: String) : SearchSuggestion

  @Serializable data class CategorySuggestion(val category: Category) : SearchSuggestion

  @Serializable
  data class IngredientSuggestion(val ingredient: Ingredient) : SearchSuggestion
}
