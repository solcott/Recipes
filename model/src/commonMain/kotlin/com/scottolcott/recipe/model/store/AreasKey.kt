package com.scottolcott.recipe.model.store

import kotlinx.serialization.Serializable

@Serializable
sealed interface AreasKey {
  @Serializable data object GetAll : AreasKey

  @Serializable data class GetArea(val area: String) : AreasKey

  @Serializable data class FilterByName(val text: String) : AreasKey
}
