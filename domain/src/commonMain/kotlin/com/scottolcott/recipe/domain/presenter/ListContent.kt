package com.scottolcott.recipe.domain.presenter

import io.github.solcott.dataresult.DataError
import io.github.solcott.uistate.ContentState
import io.github.solcott.uistate.errorOrNull
import io.github.solcott.uistate.hasLoaded
import io.github.solcott.uistate.isLoading

/**
 * The three states every list tab renders, decoupled from any one screen's `CircuitUiState`.
 *
 * Each tab keeps its own state type — Circuit pairs a state with exactly one UI — but the mapping
 * from a [ContentState] onto these three cases is the same everywhere, so it lives here rather than
 * three times over.
 *
 * The order of the branches is the point. Having data wins over being in flight: [ContentState]
 * keeps the last list it loaded, so a background refresh reports it as content that is refreshing
 * rather than dropping the grid for a spinner.
 *
 * [ContentState.hasLoaded] is what separates the first two branches, not `data.isEmpty()` — an
 * empty list is a real answer, and reading it as "nothing yet" would leave a spinner over a
 * legitimately empty tab forever.
 */
internal inline fun <T, S> ContentState<List<T>>.foldToState(
  onLoading: () -> S,
  onError: (message: String) -> S,
  onContent: (items: List<T>, isRefreshing: Boolean) -> S,
): S =
  when {
    hasLoaded -> onContent(data, isLoading)
    isLoading -> onLoading()
    else -> onError(errorOrNull.toMessage())
  }

/**
 * A [DataError] as something to show a person.
 *
 * Deliberately matches what the Store-era `errorMessage` produced, so no screen's copy changed when
 * the error type did: a wrapped exception shows its own message, and a message-only failure shows
 * the message the source gave. It belongs in `:ui` with the rest of the user-facing strings once
 * these get localized — the screen states carry `String`, which is why it is here for now.
 */
internal fun DataError?.toMessage(): String =
  when (this) {
    null -> UNKNOWN_ERROR
    DataError.Network -> "No connection"
    is DataError.Http -> "Server error ($code)"
    is DataError.Api -> messages.firstOrNull() ?: UNKNOWN_ERROR
    DataError.Serialization -> "Unexpected response from the server"
    is DataError.Unknown -> message ?: UNKNOWN_ERROR
  }

private const val UNKNOWN_ERROR = "Unknown Error"
