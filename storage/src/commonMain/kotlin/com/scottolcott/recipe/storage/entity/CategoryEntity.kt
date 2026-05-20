package com.scottolcott.recipe.storage.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.scottolcott.recipe.model.CategoryId
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
@Entity(tableName = "category")
data class CategoryEntity(
  @PrimaryKey val id: CategoryId,
  val name: String,
  val thumb: String,
  val description: String,
  val lastFetched: Instant,
)
