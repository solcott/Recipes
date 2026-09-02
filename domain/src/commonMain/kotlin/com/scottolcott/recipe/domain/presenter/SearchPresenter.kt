package com.scottolcott.recipe.domain.presenter

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import com.scottolcott.recipe.domain.presenter.SearchOuterEvent.NavigateToCategoryResults
import com.scottolcott.recipe.domain.presenter.SearchOuterEvent.NavigateToIngredientResults
import com.scottolcott.recipe.domain.presenter.SearchOuterEvent.NavigateToSearchResults
import com.scottolcott.recipe.model.Category
import com.scottolcott.recipe.model.CategorySuggestions
import com.scottolcott.recipe.model.Ingredient
import com.scottolcott.recipe.model.IngredientSuggestions
import com.scottolcott.recipe.model.SearchSuggestion
import com.scottolcott.recipe.model.SearchSuggestions
import com.scottolcott.recipe.repository.SearchSuggestionsRepository
import com.slack.circuit.retained.produceRetainedState
import com.slack.circuit.subcircuit.SubCircuitInject
import com.slack.circuit.subcircuit.SubCircuitOuterEvent
import com.slack.circuit.subcircuit.SubCircuitUiEvent
import com.slack.circuit.subcircuit.SubCircuitUiState
import com.slack.circuit.subcircuit.SubPresenter
import com.slack.circuit.subcircuit.SubScreen
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.redacted.annotations.Redacted
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch

@SubCircuitInject(SearchScreen::class, AppScope::class)
@Inject
class SearchPresenter(private val searchSuggestionsRepository: SearchSuggestionsRepository) :
  SubPresenter<SearchOuterEvent, SearchState> {
  @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
  @Composable
  override fun present(outerEventSink: (SearchOuterEvent) -> Unit): SearchState {
    val scope = rememberCoroutineScope()

    val searchText = rememberTextFieldState()
    val suggestions by
      produceRetainedState(
        SearchSuggestions(
          emptyList(),
          CategorySuggestions(true, false, emptyList()),
          IngredientSuggestions(true, false, emptyList()),
        )
      ) {
        snapshotFlow { searchText.text }
          .debounce(300.milliseconds)
          .transformLatest {
            emitAll(searchSuggestionsRepository.getSearchSuggestionsAsFlow(it.toString()))
          }
          .collect { value = it }
      }
    val eventSink: (SearchEvent) -> Unit = remember {
      { event ->
        when (event) {
          is SearchEvent.PerformSearch -> {
            scope.launch {
              outerEventSink(NavigateToSearchResults(event.query))
              searchSuggestionsRepository.addSearchSuggestion(
                SearchSuggestion.QuerySuggestion(event.query)
              )
              searchText.clearText()
            }
          }

          is SearchEvent.CategoryItemClicked -> {
            outerEventSink(NavigateToCategoryResults(event.category))
            scope.launch {
              searchSuggestionsRepository.addSearchSuggestion(
                SearchSuggestion.CategorySuggestion(event.category)
              )
            }
          }
          is SearchEvent.IngredientItemClicked -> {
            outerEventSink(NavigateToIngredientResults(event.ingredient))
            scope.launch {
              searchSuggestionsRepository.addSearchSuggestion(
                SearchSuggestion.IngredientSuggestion(event.ingredient)
              )
            }
          }

          is SearchEvent.RemoveSearchSuggestion -> {
            scope.launch { searchSuggestionsRepository.removeSearchSuggestion(event.suggestion) }
          }
        }
      }
    }
    return SearchState(searchText, suggestions, eventSink)
  }
}

data class SearchState(
  val searchText: TextFieldState,
  val suggestions: SearchSuggestions,
  @Redacted val eventSink: (SearchEvent) -> Unit,
) : SubCircuitUiState

sealed interface SearchEvent : SubCircuitUiEvent {
  data class PerformSearch(val query: String) : SearchEvent

  data class CategoryItemClicked(val category: Category) : SearchEvent

  data class IngredientItemClicked(val ingredient: Ingredient) : SearchEvent

  data class RemoveSearchSuggestion(val suggestion: SearchSuggestion) : SearchEvent
}

sealed interface SearchOuterEvent : SubCircuitOuterEvent {
  data class NavigateToSearchResults(val query: String) : SearchOuterEvent

  data class NavigateToCategoryResults(val category: Category) : SearchOuterEvent

  data class NavigateToIngredientResults(val ingredient: Ingredient) : SearchOuterEvent
}

data object SearchScreen : SubScreen<SearchOuterEvent>
