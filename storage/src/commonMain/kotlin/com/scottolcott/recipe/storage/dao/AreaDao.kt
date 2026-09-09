package com.scottolcott.recipe.storage.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.scottolcott.recipe.storage.entity.AreaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AreaDao {

  @Query("SELECT * FROM area ORDER BY area") fun getAllAreasAsFlow(): Flow<List<AreaEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(entities: Collection<AreaEntity>)

  @Query("DELETE FROM area") suspend fun deleteAll()
}
