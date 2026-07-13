package com.scottolcott.recipe.model

import kotlin.time.Instant

data class Ingredient(
  val id: IngredientId,
  val name: String,
  val description: String? = null,
  val type: String? = null,
  val thumbnail: String? = null,
  val lastFetched: Instant,
)
