package com.scottolcott.recipe.storage

import androidx.datastore.core.Storage
import androidx.datastore.core.okio.WebLocalStorage
import androidx.room3.Room
import androidx.room3.RoomDatabase
import com.scottolcott.recipe.storage.datastore.RecipeFetchHistory
import com.scottolcott.recipe.storage.datastore.RecipeFetchHistoryJsonSerializer
import com.scottolcott.recipe.storage.datastore.SearchSuggestions
import com.scottolcott.recipe.storage.datastore.SuggestionsJsonSerializer
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@SingleIn(AppScope::class)
@Inject
actual class StorageFactory(
  private val suggestionsSerializer: SuggestionsJsonSerializer,
  private val historySerializer: RecipeFetchHistoryJsonSerializer,
) {

  actual fun createRoomDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {

    return Room.databaseBuilder<AppDatabase>(
      name = "recipe.db",
      factory = { AppDatabaseConstructor.initialize() },
    )
  }

  actual fun createSearchSuggestionsDataStoreStorage(): Storage<SearchSuggestions> {
    return WebLocalStorage(suggestionsSerializer, name = "search_suggestions.json")
  }

  actual fun createRecipeFetchHistoryDataStoreStorage(): Storage<RecipeFetchHistory> {
    return WebLocalStorage(historySerializer, name = "recipe_fetch_history.json")
  }
}
