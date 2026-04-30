package com.scottolcott.recipe.storage.entity

import androidx.room3.ColumnInfo
import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.ForeignKey.Companion.CASCADE
import androidx.room3.PrimaryKey
import androidx.room3.Relation
import com.scottolcott.recipe.model.RecipeId
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class RecipeEntityWithDetail(
  @Embedded val recipe: RecipeEntity,
  @Relation(parentColumn = "id", entityColumn = "recipe_id") val detail: RecipeDetailEntity?,
)

@Entity(tableName = "recipe")
data class RecipeEntity
@OptIn(ExperimentalTime::class)
constructor(
  @PrimaryKey val id: RecipeId,
  val name: String,
  val thumbnail: String,
  val category: String?,
  val area: String?,
  val lastFetched: Instant,
)

@Entity(
  tableName = "recipe_detail",
  foreignKeys =
    [
      ForeignKey(
        entity = RecipeEntity::class,
        parentColumns = ["id"],
        childColumns = ["recipe_id"],
        onDelete = CASCADE,
      )
    ],
)
data class RecipeDetailEntity(
  @PrimaryKey @ColumnInfo("recipe_id") val id: RecipeId,
  @ColumnInfo(name = "alternate_name") val alternateName: String?,
  val instructions: String,
  val tags: String?,
  val youtube: String?,
  val source: String?,
  @ColumnInfo(name = "image_source") val imageSource: String?,
  @ColumnInfo(name = "creative_commons_confirmed") val creativeCommonsConfirmed: String?,
  @ColumnInfo(name = "date_modified") val dateModified: String?,
  val ingredient1: String?,
  val measure1: String?,
  val ingredient2: String?,
  val measure2: String?,
  val ingredient3: String?,
  val measure3: String?,
  val ingredient4: String?,
  val measure4: String?,
  val ingredient5: String?,
  val measure5: String?,
  val ingredient6: String?,
  val measure6: String?,
  val ingredient7: String?,
  val measure7: String?,
  val ingredient8: String?,
  val measure8: String?,
  val ingredient9: String?,
  val measure9: String?,
  val ingredient10: String?,
  val measure10: String?,
  val ingredient11: String?,
  val measure11: String?,
  val ingredient12: String?,
  val measure12: String?,
  val ingredient13: String?,
  val measure13: String?,
  val ingredient14: String?,
  val measure14: String?,
  val ingredient15: String?,
  val measure15: String?,
  val ingredient16: String?,
  val measure16: String?,
  val ingredient17: String?,
  val measure17: String?,
  val ingredient18: String?,
  val measure18: String?,
  val ingredient19: String?,
  val measure19: String?,
  val ingredient20: String?,
  val measure20: String?,
  val lastFetched: Instant,
)
