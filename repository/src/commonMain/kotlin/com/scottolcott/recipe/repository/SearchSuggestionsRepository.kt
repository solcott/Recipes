package com.scottolcott.recipe.repository

import com.scottolcott.recipe.storage.datastore.SearchSearchSuggestionsDataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface SearchSuggestionsRepository {

  suspend fun addSearchSuggestion(suggestion: String)

  fun getSearchSuggestionsAsFlow(query: String): Flow<List<String>>
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
internal class SearchSuggestionsRepositoryImpl(
  private val suggestionsDataStore: SearchSearchSuggestionsDataStore
) : SearchSuggestionsRepository {
  override suspend fun addSearchSuggestion(suggestion: String) {
    suggestionsDataStore.add(suggestion.trim())
  }

  override fun getSearchSuggestionsAsFlow(query: String): Flow<List<String>> {
    return suggestionsDataStore.suggestions.map { searchSuggestions ->
      searchSuggestions.suggestions
        .filter { suggestion -> suggestion.startsWith(query.trim()) }
        .sortedWith(String.CASE_INSENSITIVE_ORDER)
    }
  }
}
