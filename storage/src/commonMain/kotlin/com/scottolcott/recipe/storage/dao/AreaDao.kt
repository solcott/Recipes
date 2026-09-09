package com.scottolcott.recipe.storage.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.scottolcott.recipe.storage.entity.AreaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AreaDao {

  @Query("SELECT * FROM AREA ORDER BY AREA") fun getAllAreasAsFlow(): Flow<List<AreaEntity>>

  @Query("SELECT * FROM AREA WHERE AREA = :area ORDER BY AREA limit 1")
  fun getAreaAsFlow(area: String): Flow<AreaEntity?>

  @Query(
    """
    SELECT * FROM AREA WHERE AREA LIKE :query || '%'
    UNION ALL
    SELECT * FROM AREA WHERE AREA LIKE '%' || :query || '%' AND AREA NOT LIKE :query || '%'
  """
  )
  fun filterByName(query: String): Flow<List<AreaEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(entity: AreaEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(entity: Collection<AreaEntity>)

  @Query("DELETE FROM AREA") suspend fun deleteAll()

  @Query("DELETE FROM AREA WHERE AREA = :area") suspend fun deleteArea(area: String)

  @Query("DELETE FROM AREA WHERE AREA LIKE '%' || :name || '%'")
  suspend fun deleteWhereNameLike(name: String)
}
