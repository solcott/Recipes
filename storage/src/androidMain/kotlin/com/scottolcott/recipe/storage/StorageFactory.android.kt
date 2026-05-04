package com.scottolcott.recipe.storage

import android.app.Application
import androidx.datastore.core.Storage
import androidx.datastore.core.okio.OkioStorage
import androidx.room3.Room
import androidx.room3.RoomDatabase
import com.scottolcott.recipe.storage.datastore.SearchSuggestions
import com.scottolcott.recipe.storage.datastore.SuggestionsJsonSerializer
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import okio.FileSystem
import okio.Path.Companion.toPath

@SingleIn(AppScope::class)
@Inject
actual class StorageFactory(private val context: Application) {
  actual fun createRoomDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("recipe.db")
    return Room.databaseBuilder<AppDatabase>(context = appContext, name = dbFile.absolutePath,
        factory = {
            AppDatabaseConstructor.initialize()
        })
  }

  actual fun createSearchSuggestionsDataStoreStorage(): Storage<SearchSuggestions> {
      return OkioStorage(
          serializer = SuggestionsJsonSerializer,
          fileSystem = FileSystem.SYSTEM,
          producePath = { context.filesDir.resolve("search_suggestions.json").absolutePath.toPath() },
      )
  }
}
