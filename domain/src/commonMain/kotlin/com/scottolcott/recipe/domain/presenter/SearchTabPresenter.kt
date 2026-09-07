package com.scottolcott.recipe.domain.presenter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.serialization.CircuitSerializable
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.redacted.annotations.Redacted

/**
 * The Search section, as a destination rather than a control in the app bar.
 *
 * On a layout with a navigation rail the search field is docked in the bar permanently and there is
 * nothing for this to add. A tab bar has no room for that, and iOS answers with a Search *tab* -- a
 * place you go, not a button you press -- so this exists to be that place.
 *
 * It owns no search logic. The field, the suggestions, the history and the debounce all belong to
 * `SearchPresenter`, which this screen's UI hosts through `SubCircuitContent` exactly as
 * `RecipeAppBar` does. All this presenter contributes is a navigator for the results.
 */
@CircuitInject(SearchTabScreen::class, AppScope::class)
@Inject
class SearchTabPresenter internal constructor(private val navigator: Navigator) :
  Presenter<SearchTabState> {
  @Composable
  override fun present(): SearchTabState =
    SearchTabState(
      eventSink =
        remember(navigator) {
          { event ->
            when (event) {
              is SearchTabEvent.GoTo -> navigator.goTo(event.screen)
            }
          }
        }
    )
}

sealed interface SearchTabEvent : CircuitUiEvent {
  data class GoTo(val screen: Screen) : SearchTabEvent
}

data class SearchTabState(@Redacted val eventSink: (SearchTabEvent) -> Unit) : CircuitUiState

@CircuitSerializable(AppScope::class) data object SearchTabScreen : Screen
