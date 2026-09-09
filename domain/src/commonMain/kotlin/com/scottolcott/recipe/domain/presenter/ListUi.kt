package com.scottolcott.recipe.domain.presenter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import org.mobilenativefoundation.store.store5.StoreReadResponse

/**
 * The three states every list tab renders, decoupled from any one screen's `CircuitUiState`.
 *
 * Each tab keeps its own state type -- Circuit pairs a state with exactly one UI -- but the mapping
 * from a Store stream onto these three cases is the same everywhere, so it lives here rather than
 * three times over. See [rememberListUi].
 */
internal sealed interface ListUi<out T> {
  data object Loading : ListUi<Nothing>

  data class Content<T>(val items: List<T>, val isRefreshing: Boolean) : ListUi<T>

  data class Failure(val message: String) : ListUi<Nothing>
}

/**
 * Collapses a Store response into [ListUi], holding the last list it saw.
 *
 * That hold is the point: Store emits `Loading` again whenever it goes back to the network, and
 * without the retained copy a background refresh would replace a full grid with a spinner. Instead
 * the stale list stays on screen as [ListUi.Content] with `isRefreshing` set.
 *
 * Retained against [retryTrigger] so an explicit retry starts from a clean slate rather than
 * showing the list that just failed.
 */
@Composable
internal fun <T> rememberListUi(
  response: StoreReadResponse<List<T>>,
  retryTrigger: Int,
): ListUi<T> {
  var lastItems by retain(retryTrigger) { mutableStateOf<List<T>?>(null) }
  if (response is StoreReadResponse.Data) {
    lastItems = response.value
  }
  return when (response) {
    is StoreReadResponse.Initial,
    is StoreReadResponse.Loading,
    is StoreReadResponse.NoNewData ->
      lastItems?.let { ListUi.Content(it, isRefreshing = true) } ?: ListUi.Loading

    is StoreReadResponse.Data -> ListUi.Content(response.value, isRefreshing = false)

    is StoreReadResponse.Error.Exception ->
      ListUi.Failure(response.error.message ?: "Unknown error")

    is StoreReadResponse.Error.Message -> ListUi.Failure(response.message)
    // TODO not sure what to do here
    is StoreReadResponse.Error.Custom<*> -> ListUi.Failure(response.toString())
  }
}
