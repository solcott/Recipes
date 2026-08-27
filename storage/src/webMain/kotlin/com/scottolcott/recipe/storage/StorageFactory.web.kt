package com.scottolcott.recipe.storage

import androidx.datastore.core.Storage
import androidx.datastore.core.okio.WebLocalStorage
import androidx.room3.Room
import androidx.room3.RoomDatabase
import com.scottolcott.recipe.storage.datastore.CategoriesFetchHistory
import com.scottolcott.recipe.storage.datastore.CategoriesFetchHistoryJsonSerializer
import com.scottolcott.recipe.storage.datastore.IngredientsFetchHistory
import com.scottolcott.recipe.storage.datastore.IngredientsFetchHistoryJsonSerializer
import com.scottolcott.recipe.storage.datastore.RecipeFetchHistory
import com.scottolcott.recipe.storage.datastore.RecipeFetchHistoryJsonSerializer
import com.scottolcott.recipe.storage.datastore.SearchHistorySuggestions
import com.scottolcott.recipe.storage.datastore.SuggestionsJsonSerializer
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@SingleIn(AppScope::class)
@Inject
actual class StorageFactory(
  private val suggestionsSerializer: SuggestionsJsonSerializer,
  private val historySerializer: RecipeFetchHistoryJsonSerializer,
  private val categoriesHistorySerializer: CategoriesFetchHistoryJsonSerializer,
  private val ingredientsHistorySerializer: IngredientsFetchHistoryJsonSerializer,
) {

  actual fun createRoomDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {

    return Room.databaseBuilder<AppDatabase>(
      name = "recipe.db",
      factory = { AppDatabaseConstructor.initialize() },
    )
  }

  actual fun createSearchSuggestionsDataStoreStorage(): Storage<SearchHistorySuggestions> {
    return WebLocalStorage(suggestionsSerializer, name = "search_suggestions.json")
  }

  actual fun createRecipeFetchHistoryDataStoreStorage(): Storage<RecipeFetchHistory> {
    return WebLocalStorage(historySerializer, name = "recipe_fetch_history.json")
  }

  actual fun createCategoriesFetchHistoryDataStoreStorage(): Storage<CategoriesFetchHistory> {
    return WebLocalStorage(categoriesHistorySerializer, name = "categories_fetch_history.json")
  }

  actual fun createIngredientsFetchHistoryDataStoreStorage(): Storage<IngredientsFetchHistory> {
    return WebLocalStorage(ingredientsHistorySerializer, name = "ingredients_fetch_history.json")
  }
}
