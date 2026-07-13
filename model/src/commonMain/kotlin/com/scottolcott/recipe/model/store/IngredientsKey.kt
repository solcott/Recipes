package com.scottolcott.recipe.model.store

import kotlinx.serialization.Serializable

@Serializable
sealed interface IngredientsKey {
  @Serializable data object GetAll : IngredientsKey

  @Serializable data class FilterByName(val text: String) : IngredientsKey
}
