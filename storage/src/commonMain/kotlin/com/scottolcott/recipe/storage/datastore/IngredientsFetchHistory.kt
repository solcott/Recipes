package com.scottolcott.recipe.storage.datastore

import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Storage
import androidx.datastore.core.okio.OkioSerializer
import com.scottolcott.recipe.model.store.IngredientsKey
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

@Serializable data class IngredientsFetchHistory(val lastFetchTimes: Map<IngredientsKey, Instant>)

@Inject
class IngredientsFetchHistoryJsonSerializer(@param:StorageJson private val json: Json) :
  OkioSerializer<IngredientsFetchHistory> {
  override val defaultValue: IngredientsFetchHistory = IngredientsFetchHistory(persistentMapOf())

  override suspend fun readFrom(source: BufferedSource): IngredientsFetchHistory {
    return try {
      json.decodeFromString<IngredientsFetchHistory>(source.readUtf8())
    } catch (_: Exception) {
      defaultValue
    }
  }

  override suspend fun writeTo(t: IngredientsFetchHistory, sink: BufferedSink) {
    sink.use { it.writeUtf8(json.encodeToString(IngredientsFetchHistory.serializer(), t)) }
  }
}

class IngredientsFetchHistoryDataStore(private val storage: Storage<IngredientsFetchHistory>) {
  private val dataStore = DataStoreFactory.create(storage = storage)

  val history: Flow<IngredientsFetchHistory>
    get() = dataStore.data

  suspend fun updateLastFetchTime(
    key: IngredientsKey,
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

  suspend fun getLastFetchTime(key: IngredientsKey): Instant? {
    return history.first().lastFetchTimes[key]
  }

  fun refreshNeeded(key: IngredientsKey, cacheExpiration: Duration): Flow<Boolean> {
    return history.map { history ->
      val lastFetch = history.lastFetchTimes[key]
      lastFetch == null || lastFetch.plus(cacheExpiration) < Clock.System.now()
    }
  }
}
