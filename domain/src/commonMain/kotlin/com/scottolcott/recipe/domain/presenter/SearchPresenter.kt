package com.scottolcott.recipe.domain.presenter

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import com.scottolcott.recipe.domain.presenter.SearchOuterEvent.NavigateToCategoryResults
import com.scottolcott.recipe.domain.presenter.SearchOuterEvent.NavigateToIngredientResults
import com.scottolcott.recipe.domain.presenter.SearchOuterEvent.NavigateToSearchResults
import com.scottolcott.recipe.model.Category
import com.scottolcott.recipe.model.Ingredient
import com.scottolcott.recipe.model.SearchSuggestion
import com.scottolcott.recipe.repository.SearchSuggestionsRepository
import com.slack.circuit.subcircuit.SubCircuitInject
import com.slack.circuit.subcircuit.SubCircuitOuterEvent
import com.slack.circuit.subcircuit.SubCircuitUiEvent
import com.slack.circuit.subcircuit.SubCircuitUiState
import com.slack.circuit.subcircuit.SubPresenter
import com.slack.circuit.subcircuit.SubScreen
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.redacted.annotations.Redacted
import io.github.solcott.uistate.ContentStates3
import io.github.solcott.uistate.circuit.produceRetainedContentStates
import io.github.solcott.uistate.contentStatesOf
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@SubCircuitInject(SearchScreen::class, AppScope::class)
@Inject
class SearchPresenter(
  private val screen: SearchScreen,
  private val searchSuggestionsRepository: SearchSuggestionsRepository,
) : SubPresenter<SearchOuterEvent, SearchState> {
  @OptIn(FlowPreview::class, ExperimentalMaterial3Api::class)
  @Composable
  override fun present(outerEventSink: (SearchOuterEvent) -> Unit): SearchState {
    @Suppress("NoNameShadowing") val outerEventSink by rememberUpdatedState(outerEventSink)
    val scope = rememberCoroutineScope()
    val searchBarState = rememberSearchBarState(initialValue = screen.initialSearchBarValue)
    LaunchedEffect(Unit) {
      snapshotFlow { searchBarState.targetValue }
        .distinctUntilChanged()
        .collect { outerEventSink(SearchOuterEvent.SearchBarStateChanged(it)) }
    }
    val searchText = rememberTextFieldState()
    // One ContentState per source, so recents show at once while categories and ingredients are
    // still loading, and a new query keeps the last lists up, marked reloading, until its own
    // answers replace them.
    val suggestions = snapshotFlow {
      searchText.text.toString()
    }
      .debounce(300.milliseconds)
      .produceRetainedContentStates(
        contentStatesOf(
          emptyList<SearchSuggestion>(),
          emptyList<Category>(),
          emptyList<Ingredient>(),
        )
      ) { query ->
        searchSuggestionsRepository.getSearchSuggestionsAsFlow(query)
      }
    fun eventSink(event: SearchEvent) {
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
    return SearchState(searchBarState, searchText, suggestions, ::eventSink)
  }
}

/**
 * One state per suggestion source, in the order the repository combines them: stored history,
 * categories, ingredients. Destructure it to name them; `isAnyLoading` and `errorOrNull` on the
 * group answer for all three.
 */
typealias SearchSuggestionStates =
  ContentStates3<List<SearchSuggestion>, List<Category>, List<Ingredient>>

data class SearchState
@OptIn(ExperimentalMaterial3Api::class)
constructor(
  val searchBarState: SearchBarState,
  val searchText: TextFieldState,
  val suggestions: SearchSuggestionStates,
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

  data class SearchBarStateChanged
  @OptIn(ExperimentalMaterial3Api::class)
  constructor(val searchBarValue: SearchBarValue) : SearchOuterEvent
}

@OptIn(ExperimentalMaterial3Api::class)
data class SearchScreen(val initialSearchBarValue: SearchBarValue) : SubScreen<SearchOuterEvent>
