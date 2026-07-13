package com.scottolcott.recipe.model.store

import kotlinx.serialization.Serializable

@Serializable
sealed interface CategoriesKey {
  @Serializable data object GetCategories : CategoriesKey
}
