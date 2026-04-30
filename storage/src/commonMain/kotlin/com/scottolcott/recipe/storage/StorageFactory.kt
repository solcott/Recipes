package com.scottolcott.recipe.storage

import androidx.datastore.core.Storage
import androidx.room3.RoomDatabase
import com.scottolcott.recipe.storage.datastore.RecipeFavorites
import com.scottolcott.recipe.storage.datastore.SearchSuggestions

expect class StorageFactory {

  fun createRoomDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>

  fun createFavoritesDataStoreStorage(): Storage<RecipeFavorites>

  fun createSearchSuggestionsDataStoreStorage(): Storage<SearchSuggestions>
}
