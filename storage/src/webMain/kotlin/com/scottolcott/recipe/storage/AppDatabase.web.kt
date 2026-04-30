package com.scottolcott.recipe.storage

import androidx.sqlite.SQLiteDriver
import com.scottolcott.recipe.worker.createSQLiteWasmWorker
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers

actual fun getSqliteDriver(): SQLiteDriver {
  return createSQLiteWasmWorker()
}

actual fun getRoomCoroutineContext(): CoroutineContext {
  return Dispatchers.Default
}
