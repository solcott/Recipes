package com.scottolcott.recipe.storage.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.scottolcott.recipe.storage.entity.IngredientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IngredientDao {

  @Query("SELECT * FROM INGREDIENT ORDER BY ID")
  fun getAllIngredientsAsFlow(): Flow<List<IngredientEntity>>

  @Query("SELECT * FROM INGREDIENT WHERE NAME LIKE '%' || :name || '%' ORDER BY ID")
  fun filterByName(name: String): Flow<List<IngredientEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(entity: IngredientEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(entity: Collection<IngredientEntity>)

  @Query("DELETE FROM INGREDIENT") suspend fun deleteAll()

  @Query("DELETE FROM INGREDIENT WHERE NAME LIKE '%' || :name || '%'")
  suspend fun deleteWhereNameLike(name: String)
}
