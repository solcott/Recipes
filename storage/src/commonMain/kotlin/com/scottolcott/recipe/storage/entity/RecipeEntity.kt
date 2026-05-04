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
  @Relation(parentColumn = "recipe_id", entityColumn = "recipe_detail_recipe_id")
  val detail: RecipeDetailEntity?,
  @Relation(parentColumn = "recipe_id", entityColumn = "favorite_recipe_id")
  val favorite: FavoriteEntity?,
)

@Entity(tableName = "recipe")
data class RecipeEntity
@OptIn(ExperimentalTime::class)
constructor(
  @PrimaryKey @ColumnInfo(name = "recipe_id") val id: RecipeId,
  @ColumnInfo(name = "recipe_name") val name: String,
  @ColumnInfo(name = "recipe_thumbnail") val thumbnail: String,
  @ColumnInfo(name = "recipe_category") val category: String?,
  @ColumnInfo(name = "recipe_area") val area: String?,
  @ColumnInfo(name = "recipe_last_fetched") val lastFetched: Instant,
)

@Entity(
  tableName = "recipe_detail",
  foreignKeys =
    [
      ForeignKey(
        entity = RecipeEntity::class,
        parentColumns = ["recipe_id"],
        childColumns = ["recipe_detail_recipe_id"],
        onDelete = CASCADE,
      )
    ],
)
data class RecipeDetailEntity(
  @PrimaryKey @ColumnInfo("recipe_detail_recipe_id") val id: RecipeId,
  @ColumnInfo(name = "recipe_detail_alternate_name") val alternateName: String?,
  @ColumnInfo(name = "recipe_detail_instructions") val instructions: String,
  @ColumnInfo(name = "recipe_detail_tags") val tags: String?,
  @ColumnInfo(name = "recipe_detail_youtube") val youtube: String?,
  @ColumnInfo(name = "recipe_detail_source") val source: String?,
  @ColumnInfo(name = "recipe_detail_image_source") val imageSource: String?,
  @ColumnInfo(name = "recipe_detail_creative_commons_confirmed")
  val creativeCommonsConfirmed: String?,
  @ColumnInfo(name = "recipe_detail_date_modified") val dateModified: String?,
  @ColumnInfo(name = "recipe_detail_ingredient1") val ingredient1: String?,
  @ColumnInfo(name = "recipe_detail_measure1") val measure1: String?,
  @ColumnInfo(name = "recipe_detail_ingredient2") val ingredient2: String?,
  @ColumnInfo(name = "recipe_detail_measure2") val measure2: String?,
  @ColumnInfo(name = "recipe_detail_ingredient3") val ingredient3: String?,
  @ColumnInfo(name = "recipe_detail_measure3") val measure3: String?,
  @ColumnInfo(name = "recipe_detail_ingredient4") val ingredient4: String?,
  @ColumnInfo(name = "recipe_detail_measure4") val measure4: String?,
  @ColumnInfo(name = "recipe_detail_ingredient5") val ingredient5: String?,
  @ColumnInfo(name = "recipe_detail_measure5") val measure5: String?,
  @ColumnInfo(name = "recipe_detail_ingredient6") val ingredient6: String?,
  @ColumnInfo(name = "recipe_detail_measure6") val measure6: String?,
  @ColumnInfo(name = "recipe_detail_ingredient7") val ingredient7: String?,
  @ColumnInfo(name = "recipe_detail_measure7") val measure7: String?,
  @ColumnInfo(name = "recipe_detail_ingredient8") val ingredient8: String?,
  @ColumnInfo(name = "recipe_detail_measure8") val measure8: String?,
  @ColumnInfo(name = "recipe_detail_ingredient9") val ingredient9: String?,
  @ColumnInfo(name = "recipe_detail_measure9") val measure9: String?,
  @ColumnInfo(name = "recipe_detail_ingredient10") val ingredient10: String?,
  @ColumnInfo(name = "recipe_detail_measure10") val measure10: String?,
  @ColumnInfo(name = "recipe_detail_ingredient11") val ingredient11: String?,
  @ColumnInfo(name = "recipe_detail_measure11") val measure11: String?,
  @ColumnInfo(name = "recipe_detail_ingredient12") val ingredient12: String?,
  @ColumnInfo(name = "recipe_detail_measure12") val measure12: String?,
  @ColumnInfo(name = "recipe_detail_ingredient13") val ingredient13: String?,
  @ColumnInfo(name = "recipe_detail_measure13") val measure13: String?,
  @ColumnInfo(name = "recipe_detail_ingredient14") val ingredient14: String?,
  @ColumnInfo(name = "recipe_detail_measure14") val measure14: String?,
  @ColumnInfo(name = "recipe_detail_ingredient15") val ingredient15: String?,
  @ColumnInfo(name = "recipe_detail_measure15") val measure15: String?,
  @ColumnInfo(name = "recipe_detail_ingredient16") val ingredient16: String?,
  @ColumnInfo(name = "recipe_detail_measure16") val measure16: String?,
  @ColumnInfo(name = "recipe_detail_ingredient17") val ingredient17: String?,
  @ColumnInfo(name = "recipe_detail_measure17") val measure17: String?,
  @ColumnInfo(name = "recipe_detail_ingredient18") val ingredient18: String?,
  @ColumnInfo(name = "recipe_detail_measure18") val measure18: String?,
  @ColumnInfo(name = "recipe_detail_ingredient19") val ingredient19: String?,
  @ColumnInfo(name = "recipe_detail_measure19") val measure19: String?,
  @ColumnInfo(name = "recipe_detail_ingredient20") val ingredient20: String?,
  @ColumnInfo(name = "recipe_detail_measure20") val measure20: String?,
  @ColumnInfo(name = "recipe_detail_last_fetched") val lastFetched: Instant,
)

@Entity(tableName = "favorite_recipe")
data class FavoriteEntity(
  @PrimaryKey @ColumnInfo("favorite_recipe_id") val id: RecipeId,
  @ColumnInfo("favorite_recipe_added_date_time") val addedDateTime: Instant,
)
