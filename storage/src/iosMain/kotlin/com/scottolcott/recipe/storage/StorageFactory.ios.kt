package com.scottolcott.recipe.storage

import androidx.datastore.core.Storage
import androidx.datastore.core.okio.OkioStorage
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
import kotlinx.cinterop.ExperimentalForeignApi
import okio.FileSystem
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@SingleIn(AppScope::class)
@Inject
actual class StorageFactory(
  private val suggestionsSerializer: SuggestionsJsonSerializer,
  private val historySerializer: RecipeFetchHistoryJsonSerializer,
  private val categoriesHistorySerializer: CategoriesFetchHistoryJsonSerializer,
  private val ingredientsHistorySerializer: IngredientsFetchHistoryJsonSerializer,
) {
  actual fun createRoomDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dbFilePath = documentDirectory() + "/" + DATABASE_NAME
    return Room.databaseBuilder<AppDatabase>(
      name = dbFilePath,
      factory = { AppDatabaseConstructor.initialize() },
    )
  }

  actual fun createSearchSuggestionsDataStoreStorage(): Storage<SearchHistorySuggestions> {
    return OkioStorage(
      fileSystem = FileSystem.SYSTEM,
      serializer = suggestionsSerializer,
      producePath = { (documentDirectory() + "/search_suggestions.json").toPath() },
    )
  }

  actual fun createRecipeFetchHistoryDataStoreStorage(): Storage<RecipeFetchHistory> {
    return OkioStorage(
      fileSystem = FileSystem.SYSTEM,
      serializer = historySerializer,
      producePath = { (documentDirectory() + "/recipe_fetch_history.json").toPath() },
    )
  }

  actual fun createCategoriesFetchHistoryDataStoreStorage(): Storage<CategoriesFetchHistory> {
    return OkioStorage(
      fileSystem = FileSystem.SYSTEM,
      serializer = categoriesHistorySerializer,
      producePath = { (documentDirectory() + "/categories_fetch_history.json").toPath() },
    )
  }

  actual fun createIngredientsFetchHistoryDataStoreStorage(): Storage<IngredientsFetchHistory> {
    return OkioStorage(
      fileSystem = FileSystem.SYSTEM,
      serializer = ingredientsHistorySerializer,
      producePath = { (documentDirectory() + "/ingredients_fetch_history.json").toPath() },
    )
  }

  @OptIn(ExperimentalForeignApi::class)
  private fun documentDirectory(): String {
    val documentDirectory =
      NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
      )
    return requireNotNull(documentDirectory?.path)
  }
}
