package com.scottolcott.recipe.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AreaDto(
  @SerialName("strArea") val area: String,
  @SerialName("strCountry") val country: String,
)

@Serializable data class AreaResponseDto(@SerialName("meals") val meals: List<AreaDto>?)
