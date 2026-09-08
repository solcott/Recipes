package com.scottolcott.recipe.storage.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import kotlin.time.Instant

@Entity(tableName = "area")
data class AreaEntity(
  @PrimaryKey val area: String,
  val country: String,
  val lastFetched: Instant,
)
