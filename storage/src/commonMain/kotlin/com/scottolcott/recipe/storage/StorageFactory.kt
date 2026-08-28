package com.scottolcott.recipe.storage

import androidx.datastore.core.Storage
import androidx.room3.RoomDatabase
import com.scottolcott.recipe.storage.datastore.CategoriesFetchHistory
import com.scottolcott.recipe.storage.datastore.IngredientsFetchHistory
import com.scottolcott.recipe.storage.datastore.RecipeFetchHistory
import com.scottolcott.recipe.storage.datastore.SearchHistorySuggestions

/** Filename of the Room database, identical on every platform. */
internal const val DATABASE_NAME = "recipe.db"

/** Filename of the search-suggestion DataStore, identical on every platform. */
internal const val SEARCH_SUGGESTIONS_FILE = "search_suggestions.json"

/** Filename of the recipe fetch-history DataStore, identical on every platform. */
internal const val RECIPE_FETCH_HISTORY_FILE = "recipe_fetch_history.json"

/** Filename of the categories fetch-history DataStore, identical on every platform. */
internal const val CATEGORIES_FETCH_HISTORY_FILE = "categories_fetch_history.json"

/** Filename of the ingredients fetch-history DataStore, identical on every platform. */
internal const val INGREDIENTS_FETCH_HISTORY_FILE = "ingredients_fetch_history.json"

expect class StorageFactory {

  fun createRoomDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>

  fun createSearchSuggestionsDataStoreStorage(): Storage<SearchHistorySuggestions>

  fun createRecipeFetchHistoryDataStoreStorage(): Storage<RecipeFetchHistory>

  fun createCategoriesFetchHistoryDataStoreStorage(): Storage<CategoriesFetchHistory>

  fun createIngredientsFetchHistoryDataStoreStorage(): Storage<IngredientsFetchHistory>
}
