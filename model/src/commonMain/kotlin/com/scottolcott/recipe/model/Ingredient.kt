package com.scottolcott.recipe.model

import androidx.compose.runtime.Immutable
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
@Immutable
data class Ingredient(
  val id: IngredientId,
  val name: String,
  val description: String? = null,
  val type: String? = null,
  val thumbnail: String? = null,
  val lastFetched: Instant,
)
