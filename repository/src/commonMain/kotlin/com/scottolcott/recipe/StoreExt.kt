package com.scottolcott.recipe

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import org.mobilenativefoundation.store.store5.StoreReadResponse
import org.mobilenativefoundation.store.store5.StoreReadResponse.Data
import org.mobilenativefoundation.store.store5.StoreReadResponse.Loading
import org.mobilenativefoundation.store.store5.StoreReadResponse.NoNewData

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
  get() = this is Loading

val StoreReadResponse<*>.isError: Boolean
  get() = this is StoreReadResponse.Error

fun <T> StoreReadResponse<*>.swapType(): StoreReadResponse<T> =
  when (this) {
    is StoreReadResponse.Error -> this
    is Loading -> this
    is NoNewData -> this
    is Data -> error("cannot swap type for StoreResponse.Data")
    is StoreReadResponse.Initial -> this
  }
