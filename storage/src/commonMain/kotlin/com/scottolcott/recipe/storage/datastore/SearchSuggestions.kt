package com.scottolcott.recipe.storage.datastore

import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Storage
import androidx.datastore.core.okio.OkioSerializer
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.BufferedSink
import okio.BufferedSource
import okio.use

@Serializable data class SearchSuggestions(val suggestions: List<String>)

internal object SuggestionsJsonSerializer : OkioSerializer<SearchSuggestions> {
  override val defaultValue: SearchSuggestions = SearchSuggestions(persistentListOf())

  override suspend fun readFrom(source: BufferedSource): SearchSuggestions {
    return Json.decodeFromString<SearchSuggestions>(source.readUtf8())
  }

  override suspend fun writeTo(t: SearchSuggestions, sink: BufferedSink) {
    sink.use { it.writeUtf8(Json.encodeToString(SearchSuggestions.serializer(), t)) }
  }
}

class SearchSearchSuggestionsDataStore(private val storage: Storage<SearchSuggestions>) {
  private val dataStore = DataStoreFactory.create(storage = storage)

  val suggestions: Flow<SearchSuggestions>
    get() = dataStore.data

  suspend fun add(suggestion: String) = dataStore.updateData { prev ->
    if (!prev.suggestions.contains(suggestion)) {
      prev.copy(suggestions = prev.suggestions.toMutableList().apply { add(suggestion) })
    } else {
      prev
    }
  }

  suspend fun remove(suggestion: String) = dataStore.updateData { prev ->
    prev.copy(suggestions = prev.suggestions.toMutableList().apply { remove(suggestion) })
  }
}
