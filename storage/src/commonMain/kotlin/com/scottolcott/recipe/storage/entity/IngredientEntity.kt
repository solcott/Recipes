package com.scottolcott.recipe.storage.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.scottolcott.recipe.model.IngredientId
import kotlin.time.Clock
import kotlin.time.Instant

@Entity(tableName = "ingredient", indices = [Index(value = ["name"])])
data class IngredientEntity(
  @PrimaryKey val id: IngredientId,
  val name: String,
  val description: String? = null,
  val type: String? = null,
  val thumbnail: String? = null,
  val lastFetched: Instant = Clock.System.now(),
)
