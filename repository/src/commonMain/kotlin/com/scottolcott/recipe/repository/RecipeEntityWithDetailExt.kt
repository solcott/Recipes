package com.scottolcott.recipe.repository

import com.scottolcott.recipe.model.Recipe
import com.scottolcott.recipe.model.RecipeDetails
import com.scottolcott.recipe.model.RecipeIngredient
import com.scottolcott.recipe.storage.entity.RecipeEntityWithDetail
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal fun List<RecipeEntityWithDetail>.toModel(): ImmutableList<Recipe> = map { recipe ->
  recipe.toModel()
}
  .toImmutableList()

internal fun RecipeEntityWithDetail.toModel(): Recipe {

  val detail = detail
  // @Relation does not guarantee row order, so restore the original slot order here.
  val ingredientsList =
    ingredients
      .sortedBy { it.position }
      .map { RecipeIngredient(it.name, it.measure) }
      .toImmutableList()

  return Recipe(
    id = recipe.id,
    name = recipe.name,
    thumbnail = recipe.thumbnail,
    category = recipe.category,
    area = recipe.area,
    favorite = this.favorite != null,
    details =
      if (detail == null) {
        null
      } else {
        with(detail) {
          val tags =
            detail.tags
              ?.splitToSequence(',')
              ?.map { it.trim() }
              ?.filter { it.isNotEmpty() }
              ?.toList()
              .orEmpty()
              .toImmutableList()
          RecipeDetails(
            alternateName = alternateName,
            instructions = instructions,
            tags = tags,
            youtube = youtube,
            source = source,
            imageSource = imageSource,
            creativeCommonsConfirmed = creativeCommonsConfirmed,
            dateModified = dateModified,
            ingredients = ingredientsList,
            lastFetched = lastFetched,
          )
        }
      },
    lastFetched = recipe.lastFetched,
  )
}
