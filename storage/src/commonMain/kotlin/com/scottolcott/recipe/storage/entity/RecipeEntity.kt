package com.scottolcott.recipe.storage.entity

import androidx.room3.ColumnInfo
import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.ForeignKey.Companion.CASCADE
import androidx.room3.Index
import androidx.room3.PrimaryKey
import androidx.room3.Relation
import com.scottolcott.recipe.model.RecipeId
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class RecipeEntityWithDetail(
  @Embedded val recipe: RecipeEntity,
  @Relation(parentColumns = ["recipe_id"], entityColumns = ["recipe_detail_recipe_id"])
  val detail: RecipeDetailEntity?,
  @Relation(parentColumns = ["recipe_id"], entityColumns = ["favorite_recipe_id"])
  val favorite: FavoriteEntity?,
  @Relation(parentColumns = ["recipe_id"], entityColumns = ["recipe_ingredient_recipe_id"])
  val ingredients: List<RecipeIngredientEntity> = emptyList(),
)

@OptIn(ExperimentalTime::class)
@Entity(
  tableName = "recipe",
  indices =
    [
      Index(value = ["recipe_name"]),
      Index(value = ["recipe_category"]),
      Index(value = ["recipe_area"]),
    ],
)
data class RecipeEntity(
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
  @ColumnInfo(name = "recipe_detail_last_fetched") val lastFetched: Instant,
)

/**
 * One row per ingredient slot of a recipe, replacing TheMealDB's 20 flat `strIngredientN` /
 * `strMeasureN` columns with something queryable.
 *
 * [nameNormalized] is stored and indexed so an ingredient filter can match case- and
 * whitespace-insensitively without defeating the index; [position] preserves the original slot
 * order, which `@Relation` does not guarantee on read.
 */
@Entity(
  tableName = "recipe_ingredient",
  primaryKeys = ["recipe_ingredient_recipe_id", "recipe_ingredient_position"],
  foreignKeys =
    [
      ForeignKey(
        entity = RecipeEntity::class,
        parentColumns = ["recipe_id"],
        childColumns = ["recipe_ingredient_recipe_id"],
        onDelete = CASCADE,
      )
    ],
  indices =
    [
      Index(value = ["recipe_ingredient_name_normalized"]),
      Index(value = ["recipe_ingredient_recipe_id"]),
    ],
)
data class RecipeIngredientEntity(
  @ColumnInfo(name = "recipe_ingredient_recipe_id") val recipeId: RecipeId,
  @ColumnInfo(name = "recipe_ingredient_position") val position: Int,
  @ColumnInfo(name = "recipe_ingredient_name") val name: String,
  @ColumnInfo(name = "recipe_ingredient_name_normalized") val nameNormalized: String,
  @ColumnInfo(name = "recipe_ingredient_measure") val measure: String,
)

@Entity(tableName = "favorite_recipe")
data class FavoriteEntity(
  @PrimaryKey @ColumnInfo("favorite_recipe_id") val id: RecipeId,
  @ColumnInfo("favorite_recipe_added_date_time") val addedDateTime: Instant,
)
