package com.scottolcott.recipe.model

data class SearchSuggestions(
  val history: List<SearchSuggestion>,
  val categories: CategorySuggestions,
  val ingredientSuggestions: IngredientSuggestions,
)

data class CategorySuggestions(
  val loading: Boolean,
  val error: Boolean,
  val categories: List<Category>,
)

data class IngredientSuggestions(
  val loading: Boolean,
  val error: Boolean,
  val ingredients: List<Ingredient>,
)
