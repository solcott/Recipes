package com.scottolcott.recipe.domain.presenter

import io.github.solcott.uistate.ContentState
import io.github.solcott.uistate.errorOrNull
import io.github.solcott.uistate.isLoading

/**
 * [foldToState] for a screen that shows one optional item, such as a recipe by id.
 *
 * Simpler than the list version, because a non-null item is always an answer: the one read
 * `hasAnswer` exists to refuse -- an empty read from cache while its request is still outstanding
 * -- is a null here, and a null is never content. So having the item wins over being in flight (a
 * refresh renders as content that is refreshing, not a spinner); no item while in flight is still
 * loading; and no item once settled is an error -- the request failed, or the source answered that
 * there is no such item.
 */
internal inline fun <T : Any, S> ContentState<T?>.foldToState(
  onLoading: () -> S,
  onError: (message: String) -> S,
  onContent: (item: T, isRefreshing: Boolean) -> S,
): S {
  val item = data
  return when {
    item != null -> onContent(item, isLoading)
    isLoading -> onLoading()
    else -> onError(errorOrNull.toMessage())
  }
}
