package com.scottolcott.recipe

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import org.mobilenativefoundation.store.store5.StoreReadResponse

fun <T> Flow<StoreReadResponse<T>>.logErrors(
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

val StoreReadResponse<*>.isLoading: Boolean
  get() = this is StoreReadResponse.Loading

val StoreReadResponse<*>.isError: Boolean
  get() = this is StoreReadResponse.Error
