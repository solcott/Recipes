package com.scottolcott.recipe.storage

import com.scottolcott.recipe.serialization.StorageJson
import com.scottolcott.recipe.storage.dao.CategoryDao
import com.scottolcott.recipe.storage.dao.RecipeDao
import com.scottolcott.recipe.storage.datastore.RecipeFetchHistoryDataStore
import com.scottolcott.recipe.storage.datastore.SearchSearchSuggestionsDataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.serialization.json.Json

@ContributesTo(AppScope::class)
interface StorageProviders {
  @Provides
  @StorageJson
  @SingleIn(AppScope::class)
  fun provideStorageJson(): Json = Json {
    allowStructuredMapKeys = true
    ignoreUnknownKeys = true
  }

  @Provides
  @SingleIn(AppScope::class)
  fun provideAppDatabase(storageFactory: StorageFactory): AppDatabase =
    storageFactory
      .createRoomDatabaseBuilder()
      .setDriver(getSqliteDriver())
      .setQueryCoroutineContext(getRoomCoroutineContext())
      .build()

  @Provides fun provideRecipeDao(appDatabase: AppDatabase): RecipeDao = appDatabase.recipeDao()

  @Provides
  fun provideCategoryDao(appDatabase: AppDatabase): CategoryDao = appDatabase.categoryDao()

  @Provides
  @SingleIn(AppScope::class)
  fun provideSuggestionsDataStore(
    storageFactory: StorageFactory
  ): SearchSearchSuggestionsDataStore =
    SearchSearchSuggestionsDataStore(storageFactory.createSearchSuggestionsDataStoreStorage())

  @Provides
  @SingleIn(AppScope::class)
  fun provideRecipeFetchHistoryDataStore(
    storageFactory: StorageFactory
  ): RecipeFetchHistoryDataStore =
    RecipeFetchHistoryDataStore(storageFactory.createRecipeFetchHistoryDataStoreStorage())
}
