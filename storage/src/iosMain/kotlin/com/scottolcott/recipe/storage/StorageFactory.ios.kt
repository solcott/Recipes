package com.scottolcott.recipe.storage

import androidx.datastore.core.Storage
import androidx.datastore.core.okio.OkioStorage
import androidx.room3.Room
import androidx.room3.RoomDatabase
import com.scottolcott.recipe.storage.datastore.RecipeFetchHistory
import com.scottolcott.recipe.storage.datastore.RecipeFetchHistoryJsonSerializer
import com.scottolcott.recipe.storage.datastore.SearchSuggestions
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
) {
  actual fun createRoomDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dbFilePath = documentDirectory() + "/my_room.db"
    return Room.databaseBuilder<AppDatabase>(
      name = dbFilePath,
      factory = { AppDatabaseConstructor.initialize() },
    )
  }

  actual fun createSearchSuggestionsDataStoreStorage(): Storage<SearchSuggestions> {
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
