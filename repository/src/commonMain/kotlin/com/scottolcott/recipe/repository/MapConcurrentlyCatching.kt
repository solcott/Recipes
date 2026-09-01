package com.scottolcott.recipe.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Applies [transform] to every element with at most [concurrency] calls in flight, tolerating
 * individual failures: a failed element is reported to [onFailure] and dropped rather than
 * cancelling its siblings.
 *
 * Failure is only fatal when *every* element failed, in which case the first throwable is rethrown.
 * That guard matters for callers feeding a Store fetcher — a total outage has to surface as an
 * error, because a successfully-returned empty list gets written back and stamped as a fresh fetch,
 * suppressing any retry until the cache expires.
 *
 * A `null` returned by [transform] is a successful empty result, not a failure: it is left out of
 * the returned list but does not count towards the all-failed check.
 *
 * Note that a [Semaphore] is the tool for this and not `limitedParallelism`, which caps how many
 * threads run code in parallel rather than how many coroutines are concurrently in flight — a
 * coroutine suspended on a network call holds no thread. It is also a no-op on the single-threaded
 * web targets.
 */
internal suspend fun <T, R : Any> Collection<T>.mapConcurrentlyCatching(
  concurrency: Int,
  onFailure: (T, Throwable) -> Unit,
  transform: suspend (T) -> R?,
): List<R> {
  if (isEmpty()) return emptyList()

  val permits = Semaphore(concurrency)
  val results: List<Pair<T, Result<R?>>> = coroutineScope {
    map { item ->
      async {
        val outcome = permits.withPermit { runCatching { transform(item) } }
        // runCatching rather than a catch clause, because catching Throwable directly trips
        // detekt. ensureActive puts back what that costs us: it rethrows a CancellationException
        // that runCatching would otherwise have swallowed, keeping cancellation structured.
        if (outcome.isFailure) coroutineContext.ensureActive()
        item to outcome
      }
    }
      .awaitAll()
  }

  val failures = results.mapNotNull { (item, result) ->
    result.exceptionOrNull()?.let { item to it }
  }
  if (failures.size == results.size) throw failures.first().second
  failures.forEach { (item, error) -> onFailure(item, error) }

  return results.mapNotNull { (_, result) -> result.getOrNull() }
}
