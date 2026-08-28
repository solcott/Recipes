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
import java.io.File
import net.harawata.appdirs.AppDirsFactory
import okio.FileSystem
import okio.Path.Companion.toOkioPath

/** Directory name for the desktop app's data, under the platform's per-user data root. */
private const val APP_NAME = "Recipes"

/** Vendor segment appdirs uses on Windows; ignored on macOS and Linux. */
private const val APP_AUTHOR = "scottolcott"

// detekt 2.0.0-alpha.6 false positive: its type-resolution pass (detektMainJvm) analyses commonMain
// and jvmMain as a single module, so every actual below fails to match its expect and the class
// body stops resolving. Remove on detekt upgrade.
@Suppress("UnusedPrivateProperty")
@SingleIn(AppScope::class)
@Inject
actual class StorageFactory(
  private val suggestionsSerializer: SuggestionsJsonSerializer,
  private val historySerializer: RecipeFetchHistoryJsonSerializer,
  private val categoriesHistorySerializer: CategoriesFetchHistoryJsonSerializer,
  private val ingredientsHistorySerializer: IngredientsFetchHistoryJsonSerializer,
) {
  /**
   * The per-user data directory for this app: `~/Library/Application Support/Recipes` on macOS,
   * `%LOCALAPPDATA%\scottolcott\Recipes` on Windows, `$XDG_DATA_HOME/Recipes` on Linux.
   * Deliberately not `java.io.tmpdir`, which is cleared on reboot and would silently discard
   * favorites.
   *
   * The directory is created on first use: [OkioStorage] makes its own parent directories, but Room
   * does not, and a missing directory surfaces there as "unable to open database file".
   */
  private val appDataDirectory: File by lazy {
    File(AppDirsFactory.getInstance().getUserDataDir(APP_NAME, null, APP_AUTHOR)).apply { mkdirs() }
  }

  actual fun createRoomDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    return Room.databaseBuilder<AppDatabase>(
      name = File(appDataDirectory, DATABASE_NAME).absolutePath,
      factory = { AppDatabaseConstructor.initialize() },
    )
  }

  actual fun createSearchSuggestionsDataStoreStorage(): Storage<SearchHistorySuggestions> {
    return OkioStorage(
      serializer = suggestionsSerializer,
      fileSystem = FileSystem.SYSTEM,
      producePath = { File(appDataDirectory, SEARCH_SUGGESTIONS_FILE).toOkioPath() },
    )
  }

  actual fun createRecipeFetchHistoryDataStoreStorage(): Storage<RecipeFetchHistory> {
    return OkioStorage(
      serializer = historySerializer,
      fileSystem = FileSystem.SYSTEM,
      producePath = { File(appDataDirectory, RECIPE_FETCH_HISTORY_FILE).toOkioPath() },
    )
  }

  actual fun createCategoriesFetchHistoryDataStoreStorage(): Storage<CategoriesFetchHistory> {
    return OkioStorage(
      serializer = categoriesHistorySerializer,
      fileSystem = FileSystem.SYSTEM,
      producePath = { File(appDataDirectory, CATEGORIES_FETCH_HISTORY_FILE).toOkioPath() },
    )
  }

  actual fun createIngredientsFetchHistoryDataStoreStorage(): Storage<IngredientsFetchHistory> {
    return OkioStorage(
      serializer = ingredientsHistorySerializer,
      fileSystem = FileSystem.SYSTEM,
      producePath = { File(appDataDirectory, INGREDIENTS_FETCH_HISTORY_FILE).toOkioPath() },
    )
  }
}
