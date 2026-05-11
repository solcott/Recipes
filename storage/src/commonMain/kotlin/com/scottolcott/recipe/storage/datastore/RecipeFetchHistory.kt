package com.scottolcott.recipe.storage.datastore

import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Storage
import androidx.datastore.core.okio.OkioSerializer
import com.scottolcott.recipe.model.RecipeKey
import com.scottolcott.recipe.serialization.StorageJson
import dev.zacsweers.metro.Inject
import kotlin.time.Instant
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.BufferedSink
import okio.BufferedSource
import okio.use

@Serializable data class RecipeFetchHistory(val lastFetchTimes: Map<RecipeKey, Instant>)

@Inject
class RecipeFetchHistoryJsonSerializer(@param:StorageJson private val json: Json) :
  OkioSerializer<RecipeFetchHistory> {
  override val defaultValue: RecipeFetchHistory = RecipeFetchHistory(persistentMapOf())

  override suspend fun readFrom(source: BufferedSource): RecipeFetchHistory {
    return try {
      json.decodeFromString<RecipeFetchHistory>(source.readUtf8())
    } catch (_: Exception) {
      defaultValue
    }
  }

  override suspend fun writeTo(t: RecipeFetchHistory, sink: BufferedSink) {
    sink.use { it.writeUtf8(json.encodeToString(RecipeFetchHistory.serializer(), t)) }
  }
}

class RecipeFetchHistoryDataStore(private val storage: Storage<RecipeFetchHistory>) {
  private val dataStore = DataStoreFactory.create(storage = storage)

  val history: Flow<RecipeFetchHistory>
    get() = dataStore.data

  suspend fun updateLastFetchTime(
    key: RecipeKey,
    time: Instant,
    expirationThreshold: Instant? = null,
  ) = dataStore.updateData { prev ->
    val updatedTimes = prev.lastFetchTimes.toMutableMap().apply { put(key, time) }
    val finalTimes =
      if (expirationThreshold != null) {
        updatedTimes.filterValues { it >= expirationThreshold }
      } else {
        updatedTimes
      }
    prev.copy(lastFetchTimes = finalTimes)
  }

  suspend fun getLastFetchTime(key: RecipeKey): Instant? {
    return history.first().lastFetchTimes[key]
  }
}
