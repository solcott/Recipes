package com.scottolcott.recipe.repository

import com.scottolcott.recipe.model.normalizeIngredient
import com.scottolcott.recipe.network.dto.RecipeBasicDto
import com.scottolcott.recipe.network.dto.RecipeDto
import com.scottolcott.recipe.network.dto.RecipeFullDto
import com.scottolcott.recipe.storage.entity.RecipeDetailEntity
import com.scottolcott.recipe.storage.entity.RecipeEntity
import com.scottolcott.recipe.storage.entity.RecipeEntityWithDetail
import com.scottolcott.recipe.storage.entity.RecipeIngredientEntity
import kotlin.time.Clock
import kotlin.time.Instant

private fun RecipeFullDto.toDetailEntity(lastFetched: Instant): RecipeDetailEntity {
  return RecipeDetailEntity(
    id = id,
    alternateName = alternateName,
    instructions = instructions,
    tags = tags,
    youtube = youtube,
    source = source,
    imageSource = imageSource,
    creativeCommonsConfirmed = creativeCommonsConfirmed,
    dateModified = dateModified,
    lastFetched = lastFetched,
  )
}

/**
 * Flattens the DTO's 20 `ingredientN`/`measureN` slots into rows. Empty slots are dropped; a slot
 * with a name but no measure is kept, since the API leaves measures blank for things like "salt to
 * taste". Slot order is preserved in `position`.
 */
private fun RecipeFullDto.toIngredientEntities(): List<RecipeIngredientEntity> {
  return listOf(
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
    .mapIndexedNotNull { index, (ingredientName, measureAmount) ->
      val name = ingredientName?.trim()
      if (name.isNullOrEmpty()) {
        null
      } else {
        RecipeIngredientEntity(
          recipeId = id,
          position = index + 1,
          name = name,
          nameNormalized = name.normalizeIngredient(),
          measure = measureAmount.orEmpty().trim(),
        )
      }
    }
}

private fun RecipeFullDto.toEntityWithDetail(lastFetched: Instant): RecipeEntityWithDetail {
  return RecipeEntityWithDetail(
    RecipeEntity(
      id = id,
      name = name,
      category = this.category,
      area = this.area,
      thumbnail = thumbnail,
      lastFetched = lastFetched,
    ),
    toDetailEntity(lastFetched),
    null,
    toIngredientEntities(),
  )
}

private fun RecipeDto.toEntity(
  category: String?,
  area: String?,
  lastFetched: Instant,
): RecipeEntityWithDetail {
  return when (this) {
    is RecipeBasicDto ->
      RecipeEntityWithDetail(
        RecipeEntity(
          id = id,
          name = name,
          category = category,
          area = area,
          thumbnail = thumbnail,
          lastFetched = lastFetched,
        ),
        null,
        null,
      )
    is RecipeFullDto -> toEntityWithDetail(lastFetched)
  }
}

internal fun List<RecipeDto>.toEntities(
  category: String?,
  area: String?,
): List<RecipeEntityWithDetail> {
  val lastFetched = Clock.System.now()
  return map { it.toEntity(category, area, lastFetched) }
}
