package com.scottolcott.recipe.network.dto

import com.scottolcott.recipe.model.IngredientId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response wrapper for list.php?i=list (returns the full ingredient list). */
@Serializable data class IngredientResponseDto(val meals: List<IngredientDto>?)

@Serializable
data class IngredientDto(
  @SerialName("idIngredient") val id: IngredientId,
  @SerialName("strIngredient") val name: String,
  @SerialName("strDescription") val description: String? = null,
  @SerialName("strType") val type: String? = null,
  @SerialName("strThumb") val thumbnail: String? = null,
)
