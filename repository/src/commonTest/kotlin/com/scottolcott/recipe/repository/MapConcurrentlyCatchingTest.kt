package com.scottolcott.recipe.repository

import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private class Boom(val id: Int) : RuntimeException("boom $id")

val mapConcurrentlyCatchingTests by testSuite {
  test("allSucceed") {
    val failures = mutableListOf<Int>()

    val result =
      (1..5).toList().mapConcurrentlyCatching(
        concurrency = 2,
        onFailure = { item, _ -> failures += item },
      ) {
        "item-$it"
      }

    assertEquals(listOf("item-1", "item-2", "item-3", "item-4", "item-5"), result)
    assertTrue(failures.isEmpty())
  }

  test("partialFailureKeepsSurvivors") {
    val failures = mutableListOf<Pair<Int, Throwable>>()

    val result =
      (1..6).toList().mapConcurrentlyCatching(
        concurrency = 3,
        onFailure = { item, error -> failures += item to error },
      ) { item ->
        if (item % 2 == 0) throw Boom(item) else "item-$item"
      }

    assertEquals(listOf("item-1", "item-3", "item-5"), result)
    assertEquals(listOf(2, 4, 6), failures.map { it.first })
    assertTrue(failures.all { (item, error) -> error is Boom && error.id == item })
  }

  // A total failure has to stay fatal: an empty return would be written back as a successful fetch
  // and suppress any retry until the cache expires.
  test("allFailingThrowsFirstFailure") {
    val error =
      assertFailsWith<Boom> {
        (1..4).toList().mapConcurrentlyCatching<Int, String>(
          concurrency = 2,
          onFailure = { _, _ -> fail("onFailure must not run when the call throws") },
        ) { item ->
          throw Boom(item)
        }
      }

    assertEquals(1, error.id)
  }

  test("emptyReceiverReturnsEmpty") {
    val result =
      emptyList<Int>().mapConcurrentlyCatching<Int, String>(
        concurrency = 2,
        onFailure = { _, _ -> fail("onFailure must not run for an empty receiver") },
      ) {
        fail("transform must not run for an empty receiver")
      }

    assertEquals(emptyList(), result)
  }

  // A null is a successful empty result, so it must not count towards the all-failed check.
  test("nullResultsAreNotFailures") {
    val result =
      (1..3).toList().mapConcurrentlyCatching<Int, String>(
        concurrency = 2,
        onFailure = { _, _ -> fail("a null result is not a failure") },
      ) {
        null
      }

    assertEquals(emptyList(), result)
  }

  test("concurrencyIsCapped") {
    val started = Channel<Int>(Channel.UNLIMITED)
    val gate = CompletableDeferred<Unit>()

    coroutineScope {
      val work = async {
        (1..12).toList().mapConcurrentlyCatching(concurrency = 4, onFailure = { _, _ -> }) { item ->
          started.send(item)
          gate.await()
          item
        }
      }

      repeat(4) { started.receive() }
      // The permits are exhausted, so no fifth call can have entered transform.
      assertNull(started.tryReceive().getOrNull())

      gate.complete(Unit)
      assertEquals((1..12).toList(), work.await())
    }
  }

  test("cancellationIsNotSwallowed") {
    val slowStarted = CompletableDeferred<Unit>()
    var returned: List<Int>? = null

    coroutineScope {
      val job = launch {
        returned =
          (1..6).toList().mapConcurrentlyCatching(concurrency = 6, onFailure = { _, _ -> }) { item
            ->
            if (item > 3) {
              slowStarted.complete(Unit)
              CompletableDeferred<Unit>().await()
            }
            item
          }
      }

      slowStarted.await()
      job.cancelAndJoin()

      assertTrue(job.isCancelled)
      assertNull(returned)
    }
  }
}
