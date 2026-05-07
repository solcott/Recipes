package com.scottolcott.recipe.model

import kotlin.time.Instant

data class Recipe(
  val id: RecipeId,
  val name: String,
  val thumbnail: String,
  val category: String?,
  val area: String?,
  val favorite: Boolean,
  val details: RecipeDetails?,
  val lastFetched: Instant,
)

data class RecipeDetails(
  val alternateName: String? = null,
  val instructions: String,
  val tags: List<String> = emptyList(),
  val youtube: String? = null,
  val source: String? = null,
  val imageSource: String? = null,
  val creativeCommonsConfirmed: String? = null,
  val dateModified: String? = null,
  val ingredients: List<Ingredient> = emptyList(),
  val lastFetched: Instant,
)

data class Ingredient(val ingredient: String, val measure: String)
