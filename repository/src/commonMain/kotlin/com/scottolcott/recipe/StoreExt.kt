package com.scottolcott.recipe

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import org.mobilenativefoundation.store.store5.StoreReadResponse

/**
 * Logs the failures in a Store stream without changing it.
 *
 * Applied before `asOutcomes()`, while Store's own error types are still intact: an exception is
 * worth a stack trace, a message-only error is not, and that distinction is gone by the time both
 * have become a `DataError`.
 *
 * `internal`, because `StoreReadResponse` appears in its signature and Store5 is an implementation
 * detail of this module -- repositories expose `Flow<Outcome<T>>`. A public one would put Store
 * back on every consumer's compile classpath.
 */
internal fun <T> Flow<StoreReadResponse<T>>.logErrors(
  logger: Logger,
  message: String,
): Flow<StoreReadResponse<T>> {
  return onEach {
    when (it) {
      is StoreReadResponse.Error.Exception -> logger.e(it.error) { message }
      is StoreReadResponse.Error.Message -> logger.i { "$message: ${it.message}" }
      else -> Unit
    }
  }
}
