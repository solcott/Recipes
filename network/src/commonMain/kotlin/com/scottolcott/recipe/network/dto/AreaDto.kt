package com.scottolcott.recipe.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response wrapper for list.php?a=list. */
@Serializable data class AreaResponseDto(@SerialName("meals") val meals: List<AreaDto>?)

@Serializable
data class AreaDto(
  @SerialName("strArea") val area: String,
  // Nullable in the v2 schema and absent on some rows. `NetworkJson` sets `ignoreUnknownKeys` but
  // not `coerceInputValues`, so a non-null declaration here would fail the whole list on one row.
  @SerialName("strCountry") val country: String? = null,
)
