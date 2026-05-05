package com.scottolcott.recipe.repository

import com.scottolcott.recipe.model.Ingredient
import com.scottolcott.recipe.model.Recipe
import com.scottolcott.recipe.model.RecipeDetails
import com.scottolcott.recipe.storage.entity.RecipeDetailEntity
import com.scottolcott.recipe.storage.entity.RecipeEntityWithDetail

internal fun List<RecipeEntityWithDetail>.toModel(): List<Recipe> = map { recipe ->
  recipe.toModel()
}

internal fun RecipeEntityWithDetail.toModel(): Recipe {

  val detail = detail
  val ingredientsList = detail.toIngredients()

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

private fun RecipeDetailEntity?.toIngredients(): List<Ingredient> {
  return if (this == null) {
    emptyList()
  } else {
    listOf(
        ingredient1 to measure1,
        ingredient2 to measure2,
        ingredient3 to measure3,
        ingredient4 to measure4,
        ingredient5 to measure5,
        ingredient6 to measure6,
        ingredient7 to measure7,
        ingredient8 to measure8,
        ingredient9 to measure9,
        ingredient10 to measure10,
        ingredient11 to measure11,
        ingredient12 to measure12,
        ingredient13 to measure13,
        ingredient14 to measure14,
        ingredient15 to measure15,
        ingredient16 to measure16,
        ingredient17 to measure17,
        ingredient18 to measure18,
        ingredient19 to measure19,
        ingredient20 to measure20,
      )
      .mapNotNull { (ingredientName, measureAmount) ->
        // if ingredient name is null or empt filter it out
        if (!ingredientName.isNullOrBlank()) {
          Ingredient(
            ingredient = ingredientName,
            // This assumes measurement can be null as long as the name isn't
            measure = measureAmount.orEmpty(),
          )
        } else {
          null
        }
      }
  }
}
