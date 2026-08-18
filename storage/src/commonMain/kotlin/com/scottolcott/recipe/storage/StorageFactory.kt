package com.scottolcott.recipe.storage

import androidx.datastore.core.Storage
import androidx.room3.RoomDatabase
import com.scottolcott.recipe.storage.datastore.CategoriesFetchHistory
import com.scottolcott.recipe.storage.datastore.IngredientsFetchHistory
import com.scottolcott.recipe.storage.datastore.RecipeFetchHistory
import com.scottolcott.recipe.storage.datastore.SearchHistorySuggestions

expect class StorageFactory {

  fun createRoomDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>

  fun createSearchSuggestionsDataStoreStorage(): Storage<SearchHistorySuggestions>

  fun createRecipeFetchHistoryDataStoreStorage(): Storage<RecipeFetchHistory>

  fun createCategoriesFetchHistoryDataStoreStorage(): Storage<CategoriesFetchHistory>

  fun createIngredientsFetchHistoryDataStoreStorage(): Storage<IngredientsFetchHistory>
}
