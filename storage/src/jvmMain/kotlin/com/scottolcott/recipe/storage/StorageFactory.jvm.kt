package com.scottolcott.recipe.storage

import androidx.datastore.core.Storage
import androidx.datastore.core.okio.OkioStorage
import androidx.room3.Room
import androidx.room3.RoomDatabase
import com.scottolcott.recipe.storage.datastore.SearchSuggestions
import com.scottolcott.recipe.storage.datastore.SuggestionsJsonSerializer
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.io.File
import okio.FileSystem
import okio.Path.Companion.toOkioPath

@SingleIn(AppScope::class)
@Inject
actual class StorageFactory {
  actual fun createRoomDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    // Note: System.getProperty("java.io.tmpdir") points to the temporary folder on the system,
    // which might be cleaned upon reboot. On macOS, you can instead use the ~/Library/Application
    // Support/[your-app] folder.
    val dbFile = File(System.getProperty("java.io.tmpdir"), "my_room.db")
    return Room.databaseBuilder<AppDatabase>(
      name = dbFile.absolutePath,
      factory = { AppDatabaseConstructor.initialize() },
    )
  }

  actual fun createSearchSuggestionsDataStoreStorage(): Storage<SearchSuggestions> {
    // Note: System.getProperty("java.io.tmpdir") points to the temporary folder on the system,
    // which might be cleaned upon reboot. On macOS, you can instead use the ~/Library/Application
    // Support/[your-app] folder.
    return OkioStorage(
      serializer = SuggestionsJsonSerializer,
      fileSystem = FileSystem.SYSTEM,
      producePath = {
        File(System.getProperty("java.io.tmpdir"), "search_suggestions.json").toOkioPath()
      },
    )
  }
}
