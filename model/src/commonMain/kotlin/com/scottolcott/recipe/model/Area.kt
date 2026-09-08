package com.scottolcott.recipe.model

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Area(
  val area: String,
  val country: String,
  val lastFetched: Instant,
)
