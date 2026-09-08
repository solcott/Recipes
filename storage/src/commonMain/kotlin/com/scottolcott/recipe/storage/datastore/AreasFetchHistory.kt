package com.scottolcott.recipe.storage.datastore

import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Storage
import androidx.datastore.core.okio.OkioSerializer
import com.scottolcott.recipe.model.store.AreasKey
import com.scottolcott.recipe.serialization.StorageJson
import dev.zacsweers.metro.Inject
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.BufferedSink
import okio.BufferedSource
import okio.use

@Serializable data class AreasFetchHistory(val lastFetchTimes: Map<AreasKey, Instant>)

@Inject
class AreasFetchHistoryJsonSerializer(@param:StorageJson private val json: Json) :
  OkioSerializer<AreasFetchHistory> {
  override val defaultValue: AreasFetchHistory = AreasFetchHistory(persistentMapOf())

  override suspend fun readFrom(source: BufferedSource): AreasFetchHistory {
    return try {
      json.decodeFromString<AreasFetchHistory>(source.readUtf8())
    } catch (_: Exception) {
      defaultValue
    }
  }

  override suspend fun writeTo(t: AreasFetchHistory, sink: BufferedSink) {
    sink.use { it.writeUtf8(json.encodeToString(AreasFetchHistory.serializer(), t)) }
  }
}

class AreasFetchHistoryDataStore(private val storage: Storage<AreasFetchHistory>) {
  private val dataStore = DataStoreFactory.create(storage = storage)

  val history: Flow<AreasFetchHistory>
    get() = dataStore.data

  suspend fun updateLastFetchTime(
    key: AreasKey,
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

  suspend fun getLastFetchTime(key: AreasKey): Instant? {
    return history.first().lastFetchTimes[key]
  }

  /**
   * Whether [key] is due a network fetch, as a flow that only reports *changes* to that answer.
   *
   * `distinctUntilChanged` is load-bearing. Callers drive a Store stream through `flatMapLatest` on
   * this flow, and every fetch writes a fresh [Instant] here -- so without it, each fetch re-emits
   * the same boolean, tearing down the in-flight stream and starting another one, fetch included.
   */
  fun refreshNeeded(key: AreasKey, cacheExpiration: Duration): Flow<Boolean> {
    return history
      .map { history ->
        val lastFetch = history.lastFetchTimes[key]
        lastFetch == null || lastFetch.plus(cacheExpiration) < Clock.System.now()
      }
      .distinctUntilChanged()
  }
}
