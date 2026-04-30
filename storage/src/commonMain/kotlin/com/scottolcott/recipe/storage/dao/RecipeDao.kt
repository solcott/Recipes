package com.scottolcott.recipe.storage.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.scottolcott.recipe.model.RecipeId
import com.scottolcott.recipe.storage.entity.RecipeDetailEntity
import com.scottolcott.recipe.storage.entity.RecipeEntity
import com.scottolcott.recipe.storage.entity.RecipeEntityWithDetail
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RecipeDao {

  @Transaction
  @Query("SELECT * FROM RECIPE WHERE name LIKE '%' || :query || '%' ORDER BY id ASC")
  abstract fun queryByName(query: String): Flow<List<RecipeEntityWithDetail>>

  @Transaction
  @Query("SELECT * FROM RECIPE WHERE id = :id")
  abstract fun getById(id: RecipeId): Flow<RecipeEntityWithDetail?>

  @Transaction
  @Query("SELECT * FROM RECIPE WHERE category = :category ORDER BY id ASC")
  abstract fun getByCategory(category: String): Flow<List<RecipeEntityWithDetail>>

  @Transaction
  @Query("SELECT * FROM RECIPE WHERE area = :area ORDER BY id ASC")
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
}
