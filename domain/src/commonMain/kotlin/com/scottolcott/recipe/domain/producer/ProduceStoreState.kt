package com.scottolcott.recipe.domain.producer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.slack.circuit.retained.produceRetainedState
import kotlinx.coroutines.flow.Flow
import org.mobilenativefoundation.store.store5.StoreReadResponse

/**
 * Collects a repository's Store stream into retained Compose state, restarting on [retryTrigger].
 *
 * `NoNewData` is dropped rather than published: it means the fetch found nothing newer than what
 * the source of truth already emitted, so publishing it would only push consumers off a response
 * that still holds the data.
 */
@Composable
internal fun <T> produceStoreState(
  retryTrigger: Int,
  stream: () -> Flow<StoreReadResponse<T>>,
): StoreReadResponse<T> {
  // The effect restarts on [retryTrigger] alone, so it must not close over the lambda it was first
  // composed with -- `rememberUpdatedState` hands the collection whichever one is current.
  val currentStream by rememberUpdatedState(stream)
  val response by
    produceRetainedState<StoreReadResponse<T>>(StoreReadResponse.Initial, retryTrigger) {
      currentStream().collect { if (it !is StoreReadResponse.NoNewData) value = it }
    }
  return response
}
