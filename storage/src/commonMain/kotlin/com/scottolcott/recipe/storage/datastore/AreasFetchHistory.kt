package com.scottolcott.recipe.storage.datastore

import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Storage
import androidx.datastore.core.okio.OkioSerializer
import com.scottolcott.recipe.serialization.StorageJson
import dev.zacsweers.metro.Inject
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.BufferedSink
import okio.BufferedSource
import okio.use

@Serializable data class AreasFetchHistory(val lastFetchTime: Instant?)

@Inject
class AreasFetchHistoryJsonSerializer(@param:StorageJson private val json: Json) :
  OkioSerializer<AreasFetchHistory> {
  override val defaultValue: AreasFetchHistory = AreasFetchHistory(null)

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

  suspend fun updateLastFetchTime(time: Instant) = dataStore.updateData { prev ->
    prev.copy(lastFetchTime = time)
  }

  suspend fun getLastFetchTime(): Instant? {
    return history.first().lastFetchTime
  }

  /**
   * Whether a network fetch is needed, as a flow that only reports *changes* to that answer.
   *
   * `distinctUntilChanged` is load-bearing. Callers drive a Store stream through `flatMapLatest` on
   * this flow, and every fetch writes a fresh [Instant] here -- so without it, each fetch re-emits
   * the same boolean, tearing down the in-flight stream and starting another one, fetch included.
   */
  fun refreshNeeded(cacheExpiration: Duration): Flow<Boolean> {
    return history
      .map { history ->
        val lastFetch = history.lastFetchTime
        lastFetch == null || lastFetch.plus(cacheExpiration) < Clock.System.now()
      }
      .distinctUntilChanged()
  }
}
