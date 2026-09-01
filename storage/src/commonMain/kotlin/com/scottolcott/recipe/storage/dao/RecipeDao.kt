package com.scottolcott.recipe.storage.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.scottolcott.recipe.model.RecipeId
import com.scottolcott.recipe.storage.entity.FavoriteEntity
import com.scottolcott.recipe.storage.entity.RecipeDetailEntity
import com.scottolcott.recipe.storage.entity.RecipeEntity
import com.scottolcott.recipe.storage.entity.RecipeEntityWithDetail
import com.scottolcott.recipe.storage.entity.RecipeIngredientEntity
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow

// TooManyFunctions: a DAO is a flat list of queries by nature, and the recipe / recipe_detail /
// recipe_ingredient writes have to share one @Transaction, so splitting the class would break the
// atomicity rather than tidy anything.
@Dao
@Suppress("AbstractClassCanBeInterface", "TooManyFunctions")
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

  /**
   * Recipes containing *every* name in [ingredients], which must already be normalized (see
   * `String.normalizeIngredient()`). [requiredCount] is the size of that list: matching rows are
   * counted per recipe and only those hitting the full count survive, which is how the AND is
   * expressed in SQL.
   */
  @Transaction
  @Query(
    """SELECT r.*
        FROM recipe r
        INNER JOIN recipe_ingredient ri ON ri.recipe_ingredient_recipe_id = r.recipe_id
        WHERE ri.recipe_ingredient_name_normalized IN (:ingredients)
        GROUP BY r.recipe_id
        HAVING COUNT(DISTINCT ri.recipe_ingredient_name_normalized) = :requiredCount
        ORDER BY r.recipe_id DESC"""
  )
  abstract fun getByIngredients(
    ingredients: List<String>,
    requiredCount: Int,
  ): Flow<List<RecipeEntityWithDetail>>

  /**
   * Of [ids], those whose details were fetched after [freshAfter]. Lets a caller skip re-fetching
   * full details it already holds.
   */
  @Query(
    """SELECT recipe_detail_recipe_id 
        FROM recipe_detail 
        WHERE recipe_detail_recipe_id IN (:ids) AND recipe_detail_last_fetched > :freshAfter"""
  )
  abstract suspend fun idsWithFreshDetail(
    ids: List<RecipeId>,
    freshAfter: Instant,
  ): List<RecipeId>

  // Upsert rather than @Insert(REPLACE): SQLite implements REPLACE as delete-then-insert, which
  // fires the CASCADE on recipe_detail and recipe_ingredient. Re-inserting a summary row for an
  // already-cached recipe would therefore wipe its details and its ingredient index.
  @Upsert abstract suspend fun upsert(recipeEntity: RecipeEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  abstract suspend fun insert(recipeDetailEntity: RecipeDetailEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  abstract suspend fun insertIngredients(ingredients: List<RecipeIngredientEntity>)

  @Query("DELETE FROM recipe_ingredient WHERE recipe_ingredient_recipe_id = :recipeId")
  abstract suspend fun deleteIngredientsFor(recipeId: RecipeId)

  @Transaction
  open suspend fun insert(recipe: RecipeEntityWithDetail) {
    upsert(recipe.recipe)
    if (recipe.detail != null) {
      insert(recipe.detail)
    }
    if (recipe.ingredients.isNotEmpty()) {
      // Replace wholesale rather than upserting row by row, so a recipe that lost an ingredient
      // slot does not keep a stale row at the tail.
      deleteIngredientsFor(recipe.recipe.id)
      insertIngredients(recipe.ingredients)
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
