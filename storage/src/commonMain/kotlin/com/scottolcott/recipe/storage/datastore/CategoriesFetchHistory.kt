package com.scottolcott.recipe.storage.datastore

import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Storage
import androidx.datastore.core.okio.OkioSerializer
import com.scottolcott.recipe.model.store.CategoriesKey
import com.scottolcott.recipe.serialization.StorageJson
import dev.zacsweers.metro.Inject
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.BufferedSink
import okio.BufferedSource
import okio.use

@Serializable data class CategoriesFetchHistory(val lastFetchTimes: Map<CategoriesKey, Instant>)

@Inject
class CategoriesFetchHistoryJsonSerializer(@param:StorageJson private val json: Json) :
  OkioSerializer<CategoriesFetchHistory> {
  override val defaultValue: CategoriesFetchHistory = CategoriesFetchHistory(persistentMapOf())

  override suspend fun readFrom(source: BufferedSource): CategoriesFetchHistory {
    return try {
      json.decodeFromString<CategoriesFetchHistory>(source.readUtf8())
    } catch (_: Exception) {
      defaultValue
    }
  }

  override suspend fun writeTo(t: CategoriesFetchHistory, sink: BufferedSink) {
    sink.use { it.writeUtf8(json.encodeToString(CategoriesFetchHistory.serializer(), t)) }
  }
}

class CategoriesFetchHistoryDataStore(private val storage: Storage<CategoriesFetchHistory>) {
  private val dataStore = DataStoreFactory.create(storage = storage)

  val history: Flow<CategoriesFetchHistory>
    get() = dataStore.data

  suspend fun updateLastFetchTime(
    key: CategoriesKey,
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

  suspend fun getLastFetchTime(key: CategoriesKey): Instant? {
    return history.first().lastFetchTimes[key]
  }

  fun refreshNeeded(key: CategoriesKey, cacheExpiration: Duration): Flow<Boolean> {
    return history.map { history ->
      val lastFetch = history.lastFetchTimes[key]
      lastFetch == null || lastFetch.plus(cacheExpiration) < Clock.System.now()
    }
  }
}
