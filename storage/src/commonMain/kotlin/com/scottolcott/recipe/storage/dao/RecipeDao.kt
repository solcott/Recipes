package com.scottolcott.recipe.storage.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.scottolcott.recipe.model.RecipeId
import com.scottolcott.recipe.storage.entity.FavoriteEntity
import com.scottolcott.recipe.storage.entity.RecipeDetailEntity
import com.scottolcott.recipe.storage.entity.RecipeEntity
import com.scottolcott.recipe.storage.entity.RecipeEntityWithDetail
import kotlinx.coroutines.flow.Flow

@Dao
@Suppress("AbstractClassCanBeInterface")
abstract class RecipeDao {

  @Transaction
  @Query("SELECT * FROM recipe WHERE recipe_name LIKE '%' || :query || '%' ORDER BY recipe_id DESC")
  abstract fun queryByName(query: String): Flow<List<RecipeEntityWithDetail>>

  @Transaction
  @Query("SELECT * FROM recipe WHERE recipe_id = :id")
  abstract fun getById(id: RecipeId): Flow<RecipeEntityWithDetail?>

  @Transaction
  @Query(
    """SELECT r.* 
        FROM recipe r 
        INNER JOIN favorite_recipe f ON r.recipe_id = f.favorite_recipe_id 
        ORDER BY f.favorite_recipe_added_date_time DESC"""
  )
  abstract fun getFavorites(): Flow<List<RecipeEntityWithDetail>>

  @Transaction
  @Query("SELECT * FROM recipe WHERE recipe_category = :category ORDER BY recipe_id DESC")
  abstract fun getByCategory(category: String): Flow<List<RecipeEntityWithDetail>>

  @Transaction
  @Query("SELECT * FROM recipe WHERE recipe_area = :area ORDER BY recipe_id DESC")
  abstract fun getByArea(area: String): Flow<List<RecipeEntityWithDetail>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  abstract suspend fun insert(recipeEntity: RecipeEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  abstract suspend fun insert(recipeDetailEntity: RecipeDetailEntity)

  @Transaction
  open suspend fun insert(recipe: RecipeEntityWithDetail) {
    insert(recipe.recipe)
    if (recipe.detail != null) {
      insert(recipe.detail)
    }
  }

  @Transaction
  open suspend fun insert(recipes: List<RecipeEntityWithDetail>) {
    recipes.onEach { recipe -> insert(recipe) }
  }

  @Insert abstract suspend fun insert(recipeFavorites: FavoriteEntity)

  @Query("DELETE FROM favorite_recipe where favorite_recipe_id = :recipeId")
  abstract suspend fun deleteFavorite(recipeId: RecipeId)
}
