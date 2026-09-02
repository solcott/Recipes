package com.scottolcott.recipe.repository

import com.scottolcott.recipe.isError
import com.scottolcott.recipe.isLoading
import com.scottolcott.recipe.model.CategorySuggestions
import com.scottolcott.recipe.model.IngredientSuggestions
import com.scottolcott.recipe.model.SearchSuggestion
import com.scottolcott.recipe.model.SearchSuggestions
import com.scottolcott.recipe.storage.datastore.SearchSearchSuggestionsDataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

interface SearchSuggestionsRepository {

  suspend fun addSearchSuggestion(suggestion: SearchSuggestion)

  suspend fun removeSearchSuggestion(suggestion: SearchSuggestion)

  fun getSearchSuggestionsAsFlow(query: String): Flow<SearchSuggestions>
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
internal class SearchSuggestionsRepositoryImpl(
  private val suggestionsDataStore: SearchSearchSuggestionsDataStore,
  private val categoryRepository: CategoryRepository,
  private val ingredientRepository: IngredientRepository,
) : SearchSuggestionsRepository {
  override suspend fun addSearchSuggestion(suggestion: SearchSuggestion) {
    suggestionsDataStore.add(suggestion)
  }

  override suspend fun removeSearchSuggestion(suggestion: SearchSuggestion) {
    suggestionsDataStore.remove(suggestion)
  }

  override fun getSearchSuggestionsAsFlow(query: String): Flow<SearchSuggestions> {
    return combine(
      suggestionsDataStore.suggestions.map { searchSuggestions ->
        searchSuggestions.suggestions
          .filter { suggestion -> suggestion.text.startsWith(query.trim()) }
          .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.text })
      },
      ingredientRepository.filterIngredientsByName(query),
      categoryRepository.getCategories(query),
    ) { storedSuggestions, ingredientSuggestions, categorySuggestions ->
      SearchSuggestions(
        storedSuggestions,
        CategorySuggestions(
          categorySuggestions.isLoading,
          categorySuggestions.isError,
          categorySuggestions.dataOrNull().orEmpty(),
        ),
        IngredientSuggestions(
          ingredientSuggestions.isLoading,
          ingredientSuggestions.isError,
          ingredientSuggestions.dataOrNull().orEmpty(),
        ),
      )
    }
  }
}

private val SearchSuggestion.text: String
  get() =
    when (this) {
      is SearchSuggestion.CategorySuggestion -> category.name
      is SearchSuggestion.IngredientSuggestion -> ingredient.name
      is SearchSuggestion.QuerySuggestion -> query
    }
