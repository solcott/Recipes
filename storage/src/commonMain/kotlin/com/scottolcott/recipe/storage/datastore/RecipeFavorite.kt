package com.scottolcott.recipe.storage.datastore

import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Storage
import androidx.datastore.core.okio.OkioSerializer
import com.scottolcott.recipe.model.RecipeId
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.BufferedSink
import okio.BufferedSource
import okio.use

@Serializable data class RecipeFavorites(val favorites: List<RecipeId>)

internal object FavoritesJsonSerializer : OkioSerializer<RecipeFavorites> {
  override val defaultValue: RecipeFavorites = RecipeFavorites(persistentListOf())

  override suspend fun readFrom(source: BufferedSource): RecipeFavorites {
    return Json.decodeFromString<RecipeFavorites>(source.readUtf8())
  }

  override suspend fun writeTo(t: RecipeFavorites, sink: BufferedSink) {
    sink.use { it.writeUtf8(Json.encodeToString(RecipeFavorites.serializer(), t)) }
  }
}

class RecipeFavoritesDataStore(private val storage: Storage<RecipeFavorites>) {
  private val dataStore = DataStoreFactory.create(storage = storage)

  val favorites: Flow<RecipeFavorites>
    get() = dataStore.data

  suspend fun add(id: RecipeId) = dataStore.updateData { prev ->
    if (!prev.favorites.contains(id)) {
      prev.copy(favorites = prev.favorites.toMutableList().apply { add(id) })
    } else {
      prev
    }
  }

  suspend fun remove(id: RecipeId) = dataStore.updateData { prev ->
    prev.copy(favorites = prev.favorites.toMutableList().apply { remove(id) })
  }
}
