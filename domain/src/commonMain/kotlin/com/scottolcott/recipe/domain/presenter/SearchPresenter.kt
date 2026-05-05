package com.scottolcott.recipe.domain.presenter

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.scottolcott.recipe.repository.RecipeRepository
import com.slack.circuit.retained.produceRetainedState
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch

class SearchPresenter(
  private val navigator: Navigator,
  private val recipeRepository: RecipeRepository,
) : Presenter<SearchState> {
  @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
  @Composable
  override fun present(): SearchState {
    val scope = rememberCoroutineScope()
    var searchActive by rememberSaveable { mutableStateOf(false) }
    val searchText = rememberTextFieldState()
    val suggestions by
      produceRetainedState(emptyList()) {
        snapshotFlow { searchText.text }
          .debounce(300.milliseconds)
          .transformLatest { emitAll(recipeRepository.getSearchSuggestionsAsFlow(it.toString())) }
          .collect { value = it }
      }
    val eventSink: (SearchEvent) -> Unit = remember {
      { event ->
        when (event) {
          SearchEvent.ExitSearch -> {
            searchActive = false
            searchText.clearText()
          }
          is SearchEvent.PerformSearch -> {
            scope.launch {
              recipeRepository.addSearchSuggestion(event.query)
              navigator.goTo(RecipesScreen.BySearch(event.query))
              searchText.clearText()
              searchActive = false
            }
          }
          SearchEvent.SearchButtonClicked -> searchActive = true
        }
      }
    }
    return SearchState(searchText, searchActive, suggestions, eventSink)
  }
}

data class SearchState(
  val searchText: TextFieldState,
  val isSearchActive: Boolean,
  val suggestions: List<String>,
  val eventSink: (SearchEvent) -> Unit,
) : CircuitUiState

sealed interface SearchEvent {
  data object SearchButtonClicked : SearchEvent

  data class PerformSearch(val query: String) : SearchEvent

  data object ExitSearch : SearchEvent
}
