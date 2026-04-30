package com.scottolcott.recipe.storage

import androidx.datastore.core.Storage
import androidx.datastore.core.okio.WebLocalStorage
import androidx.room3.Room
import androidx.room3.RoomDatabase
import com.scottolcott.recipe.storage.datastore.FavoritesJsonSerializer
import com.scottolcott.recipe.storage.datastore.RecipeFavorites
import com.scottolcott.recipe.storage.datastore.SearchSuggestions
import com.scottolcott.recipe.storage.datastore.SuggestionsJsonSerializer
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@SingleIn(AppScope::class)
@Inject
actual class StorageFactory {

  actual fun createRoomDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {

    return Room.databaseBuilder<AppDatabase>(
      name = "recipe.db",
      factory = { AppDatabaseConstructor.initialize() },
    )
  }

  actual fun createFavoritesDataStoreStorage(): Storage<RecipeFavorites> {
    return WebLocalStorage(serializer = FavoritesJsonSerializer, name = "recipe_favorites.json")
  }

  actual fun createSearchSuggestionsDataStoreStorage(): Storage<SearchSuggestions> {
    return WebLocalStorage(SuggestionsJsonSerializer, name = "search_suggestions.json")
  }
}
