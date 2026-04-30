package com.scottolcott.recipe.storage

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.room3.TypeConverters
import androidx.sqlite.SQLiteDriver
import com.scottolcott.recipe.storage.dao.CategoryDao
import com.scottolcott.recipe.storage.dao.RecipeDao
import com.scottolcott.recipe.storage.entity.CategoryEntity
import com.scottolcott.recipe.storage.entity.RecipeDetailEntity
import com.scottolcott.recipe.storage.entity.RecipeEntity
import kotlin.coroutines.CoroutineContext

@Database(
  entities = [RecipeEntity::class, RecipeDetailEntity::class, CategoryEntity::class],
  version = 1,
)
@TypeConverters(RoomTypeConverters::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
  abstract fun recipeDao(): RecipeDao

  abstract fun categoryDao(): CategoryDao
}

// The Room compiler generates the `actual` implementations.
@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
  override fun initialize(): AppDatabase
}

expect fun getSqliteDriver(): SQLiteDriver

expect fun getRoomCoroutineContext(): CoroutineContext
