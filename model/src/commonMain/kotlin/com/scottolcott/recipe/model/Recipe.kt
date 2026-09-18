package com.scottolcott.recipe.model

import androidx.compose.runtime.Immutable
import kotlin.time.Instant
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
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

@Immutable
data class RecipeDetails(
  val alternateName: String? = null,
  val instructions: String,
  val tags: ImmutableList<String> = persistentListOf(),
  val youtube: String? = null,
  val source: String? = null,
  val imageSource: String? = null,
  val creativeCommonsConfirmed: String? = null,
  val dateModified: String? = null,
  val ingredients: ImmutableList<RecipeIngredient> = persistentListOf(),
  val lastFetched: Instant,
)

@Immutable data class RecipeIngredient(val ingredient: String, val measure: String)

/**
 * Canonical form of an ingredient name, used both for the indexed lookup column in storage and for
 * building [com.scottolcott.recipe.model.store.RecipesKey.ByIngredient]. The two must agree, so
 * there is exactly one of these.
 */
fun String.normalizeIngredient(): String = trim().lowercase()
