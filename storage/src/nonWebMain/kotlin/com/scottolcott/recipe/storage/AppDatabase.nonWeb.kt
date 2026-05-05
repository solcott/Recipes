package com.scottolcott.recipe.storage

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

actual fun getSqliteDriver(): androidx.sqlite.SQLiteDriver {
  return BundledSQLiteDriver()
}

actual fun getRoomCoroutineContext(): CoroutineContext {
  @Suppress("InjectDispatcher")
  return Dispatchers.IO
}
