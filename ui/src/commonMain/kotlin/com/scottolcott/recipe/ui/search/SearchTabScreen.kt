package com.scottolcott.recipe.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SearchBarValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scottolcott.recipe.domain.presenter.RecipesScreen
import com.scottolcott.recipe.domain.presenter.SearchOuterEvent
import com.scottolcott.recipe.domain.presenter.SearchScreen
import com.scottolcott.recipe.domain.presenter.SearchTabEvent
import com.scottolcott.recipe.domain.presenter.SearchTabScreen
import com.scottolcott.recipe.domain.presenter.SearchTabState
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.subcircuit.SubCircuitContent
import dev.zacsweers.metro.AppScope

/**
 * The Search tab.
 *
 * Deliberately thin: it hosts the same `SearchScreen` sub-circuit the app bar hosts, so the field,
 * the suggestion list, the history and the debounce are shared rather than reimplemented. The only
 * difference is where it sits and that it starts collapsed -- tapping the field expands it into the
 * full-screen search, which is what tapping a Search tab does on iOS.
 */
@Composable
@CircuitInject(SearchTabScreen::class, AppScope::class)
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("unused")
fun SearchTabScreen(state: SearchTabState, modifier: Modifier = Modifier) {
  Column(modifier.fillMaxSize().padding(top = 8.dp)) {
    SubCircuitContent(
      SearchScreen(initialSearchBarValue = SearchBarValue.Collapsed),
      outerEventSink = {
        val screen =
          when (it) {
            is SearchOuterEvent.NavigateToSearchResults -> RecipesScreen.BySearch(it.query)
            is SearchOuterEvent.NavigateToCategoryResults ->
              RecipesScreen.ByCategory(it.category.name)
            is SearchOuterEvent.NavigateToIngredientResults ->
              RecipesScreen.ByIngredient(setOf(it.ingredient.name))
            // The bar collapsing is how the user backs out of search; on a tab there is nothing to
            // restore, so it carries no navigation.
            is SearchOuterEvent.SearchBarStateChanged -> null
          }
        screen?.let { target -> state.eventSink(SearchTabEvent.GoTo(target)) }
      },
    )
  }
}
