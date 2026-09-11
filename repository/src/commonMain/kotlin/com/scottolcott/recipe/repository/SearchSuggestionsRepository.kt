package com.scottolcott.recipe.repository

import com.scottolcott.recipe.model.Category
import com.scottolcott.recipe.model.Ingredient
import com.scottolcott.recipe.model.SearchSuggestion
import com.scottolcott.recipe.storage.datastore.SearchSearchSuggestionsDataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.solcott.dataresult.DataError
import io.github.solcott.dataresult.Origin
import io.github.solcott.dataresult.Outcome
import io.github.solcott.dataresult.Outcomes3
import io.github.solcott.dataresult.combineOutcomes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Every source of search suggestions, emitted together: stored history, then categories, then
 * ingredients. Destructure it to name them -- `val (history, categories, ingredients) = outcomes`.
 */
typealias SearchSuggestionsOutcomes =
  Outcomes3<List<SearchSuggestion>, List<Category>, List<Ingredient>>

interface SearchSuggestionsRepository {

  suspend fun addSearchSuggestion(suggestion: SearchSuggestion)

  suspend fun removeSearchSuggestion(suggestion: SearchSuggestion)

  fun getSearchSuggestionsAsFlow(query: String): Flow<SearchSuggestionsOutcomes>
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

  // `combineOutcomes` starts every source loading, so the stored history -- local and fast -- shows
  // without waiting for categories and ingredients to answer.
  override fun getSearchSuggestionsAsFlow(query: String): Flow<SearchSuggestionsOutcomes> =
    combineOutcomes(
      storedSuggestions(query),
      categoryRepository.getCategories(query),
      ingredientRepository.filterIngredientsByName(query),
    )

  private fun storedSuggestions(query: String): Flow<Outcome<List<SearchSuggestion>>> =
    suggestionsDataStore.suggestions
      .map { stored ->
        stored.suggestions
          .filter { suggestion -> suggestion.text.startsWith(query.trim()) }
          .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.text })
      }
      .map<List<SearchSuggestion>, Outcome<List<SearchSuggestion>>> {
        Outcome.Data(it, Origin.Cache)
      }
      // Caught here rather than left to cancel the group: a store that cannot be read should cost
      // the recents section, not the categories and ingredients beside it.
      .catch { emit(Outcome.Error(DataError.Unknown(it), Origin.Cache)) }
}

private val SearchSuggestion.text: String
  get() =
    when (this) {
      is SearchSuggestion.CategorySuggestion -> category.name
      is SearchSuggestion.IngredientSuggestion -> ingredient.name
      is SearchSuggestion.QuerySuggestion -> query
    }
