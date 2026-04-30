package com.scottolcott.recipe.storage.dao

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.scottolcott.recipe.storage.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

  @Query("SELECT * FROM category") fun getCategories(): Flow<List<CategoryEntity>>

  @Query("SELECT * FROM category WHERE name LIKE :query || '%'")
  fun getCategories(query: String): Flow<List<CategoryEntity>>

  @Query("SELECT * FROM category WHERE id = :id") fun getCategory(id: Int): Flow<CategoryEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCategories(categories: List<CategoryEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCategory(category: CategoryEntity)

  @Query("DELETE FROM category") suspend fun deleteAllCategories()

  @Delete suspend fun deleteCategory(category: CategoryEntity)
}
