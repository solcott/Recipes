package com.scottolcott.recipe.domain.presenter

import io.github.solcott.uistate.ContentState
import io.github.solcott.uistate.errorOrNull
import io.github.solcott.uistate.hasAnswer
import io.github.solcott.uistate.isLoading

/**
 * The three states every screen renders, decoupled from any one screen's `CircuitUiState`.
 *
 * Each tab keeps its own state type — Circuit pairs a state with exactly one UI — but the mapping
 * from a [ContentState] onto these three cases is the same everywhere, so it lives here rather than
 * three times over.
 *
 * The order of the branches is the point. Having data wins over being in flight: [ContentState]
 * keeps the last item it loaded, so a background refresh reports it as content that is refreshing
 * rather than dropping the grid for a spinner.
 *
 * `hasAnswer` is what separates the first two branches, not `data == null` — a null is a real
 * answer, and reading it as "nothing yet" would leave a spinner over a legitimately empty tab
 * forever. What it adds over `hasLoaded` is the one empty list that is *not* an answer: an empty
 * read from cache while its request is still in flight or has failed. That is a key the database
 * has never seen, so it gets the spinner, or the error, rather than "nothing found". The
 * repositories already hold that read back with `asOutcomes(fetching = …)`; this is the backstop.
 */
internal inline fun <T, S> ContentState<T?>.foldToState(
  onLoading: () -> S,
  onError: (message: String) -> S,
  onContent: (item: T, isRefreshing: Boolean) -> S,
): S =
  when {
    hasAnswer { it != null } -> onContent(checkNotNull(data) { "data was null." }, isLoading)
    isLoading -> onLoading()
    else -> onError(errorOrNull.toMessage())
  }
