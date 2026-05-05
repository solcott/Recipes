package com.scottolcott.recipe.repository

import com.scottolcott.recipe.network.dto.RecipeBasicDto
import com.scottolcott.recipe.network.dto.RecipeDto
import com.scottolcott.recipe.network.dto.RecipeFullDto
import com.scottolcott.recipe.storage.entity.RecipeDetailEntity
import com.scottolcott.recipe.storage.entity.RecipeEntity
import com.scottolcott.recipe.storage.entity.RecipeEntityWithDetail
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
    ingredient1 = ingredient1,
    measure1 = measure1,
    ingredient2 = ingredient2,
    measure2 = measure2,
    ingredient3 = ingredient3,
    measure3 = measure3,
    ingredient4 = ingredient4,
    measure4 = measure4,
    ingredient5 = ingredient5,
    measure5 = measure5,
    ingredient6 = ingredient6,
    measure6 = measure6,
    ingredient7 = ingredient7,
    measure7 = measure7,
    ingredient8 = ingredient8,
    measure8 = measure8,
    ingredient9 = ingredient9,
    measure9 = measure9,
    ingredient10 = ingredient10,
    measure10 = measure10,
    ingredient11 = ingredient11,
    measure11 = measure11,
    ingredient12 = ingredient12,
    measure12 = measure12,
    ingredient13 = ingredient13,
    measure13 = measure13,
    ingredient14 = ingredient14,
    measure14 = measure14,
    ingredient15 = ingredient15,
    measure15 = measure15,
    ingredient16 = ingredient16,
    measure16 = measure16,
    ingredient17 = ingredient17,
    measure17 = measure17,
    ingredient18 = ingredient18,
    measure18 = measure18,
    ingredient19 = ingredient19,
    measure19 = measure19,
    ingredient20 = ingredient20,
    measure20 = measure20,
    lastFetched = lastFetched,
  )
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
