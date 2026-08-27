package com.scottolcott.recipe.storage.datastore

import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Storage
import androidx.datastore.core.okio.OkioSerializer
import com.scottolcott.recipe.model.SearchSuggestion
import com.scottolcott.recipe.serialization.StorageJson
import dev.zacsweers.metro.Inject
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.BufferedSink
import okio.BufferedSource
import okio.use

@Serializable data class SearchHistorySuggestions(val suggestions: List<SearchSuggestion>)

@Inject
class SuggestionsJsonSerializer(@param:StorageJson private val json: Json) :
  OkioSerializer<SearchHistorySuggestions> {
  override val defaultValue: SearchHistorySuggestions = SearchHistorySuggestions(persistentListOf())

  override suspend fun readFrom(source: BufferedSource): SearchHistorySuggestions {
    return json.decodeFromString<SearchHistorySuggestions>(source.readUtf8())
  }

  override suspend fun writeTo(t: SearchHistorySuggestions, sink: BufferedSink) {
    sink.use { it.writeUtf8(json.encodeToString(SearchHistorySuggestions.serializer(), t)) }
  }
}

class SearchSearchSuggestionsDataStore(private val storage: Storage<SearchHistorySuggestions>) {
  private val dataStore = DataStoreFactory.create(storage = storage)

  val suggestions: Flow<SearchHistorySuggestions>
    get() = dataStore.data

  suspend fun add(suggestion: SearchSuggestion) = dataStore.updateData { prev ->
    if (!prev.suggestions.contains(suggestion)) {
      prev.copy(suggestions = prev.suggestions.toMutableList().apply { add(suggestion) })
    } else {
      prev
    }
  }

  suspend fun remove(suggestion: SearchSuggestion) = dataStore.updateData { prev ->
    prev.copy(suggestions = prev.suggestions.toMutableList().apply { remove(suggestion) })
  }
}
