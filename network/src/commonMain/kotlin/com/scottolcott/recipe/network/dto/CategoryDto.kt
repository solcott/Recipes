package com.scottolcott.recipe.network.dto

import com.scottolcott.recipe.model.CategoryId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable data class CategoryResponseDto(val categories: List<CategoryDto>)

@Serializable
data class CategoryDto(
  @SerialName("idCategory") val id: CategoryId,
  @SerialName("strCategory") val name: String,
  @SerialName("strCategoryThumb") val thumbnail: String,
  @SerialName("strCategoryDescription") val description: String,
)
