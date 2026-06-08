package com.scottolcott.recipe

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import com.scottolcott.recipe.domain.navigation.toUrlPath
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.navigation.NavStack
import kotlinx.browser.window
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import org.w3c.dom.events.Event

/**
 * Keeps the browser's History API in sync with Circuit's [navStack] so the browser back/forward
 * buttons drive navigation.
 *
 * ## How it works
 *
 * Each Circuit screen push adds a browser history entry whose `history.state` stores the stack
 * depth as a small integer. When the user presses the browser back/forward button the browser fires
 * a `popstate` event; we read the target depth from `history.state` and call [Navigator.backward]
 * or [Navigator.forward] accordingly. Conversely, when Circuit navigates (app-initiated push or
 * pop) we push/walk browser history and set a counter so the resulting `popstate` event is ignored.
 *
 * The depth lives in `history.state` rather than the URL so the address bar shows only the screen's
 * canonical path (e.g. `/recipe/52772`). Reading and writing `history.state` is delegated to the
 * [pushDepth]/[replaceDepth]/[historyDepth] shims because kotlinx-browser types it differently on
 * the js (`Any?`) and wasmJs (`JsAny?`) targets.
 *
 * ## Navigation type detection
 *
 * The [snapshotFlow] observes BOTH [NavStack.size] and [NavStack.currentDepth] together. Comparing
 * their deltas lets us distinguish all four navigation types:
 *
 * | sizeΔ | depthΔ | type                         | browser action  |
 * |-------|--------|------------------------------|-----------------|
 * | > 0   | > 0    | push (goTo)                  | pushState       |
 * | < 0   | < 0    | pop (truncates fwd history)  | history.go(δ)   |
 * | 0     | > 0    | forward() — existing record  | history.forward |
 * | 0     | < 0    | backward() — existing record | history.back    |
 *
 * ## Counter semantics
 * - [BrowserNavState.pendingPopStateIgnore]: incremented before every [org.w3c.dom.History.go] /
 *   [org.w3c.dom.History.back] / [org.w3c.dom.History.forward] call we make so the async `popstate`
 *   it fires is silently dropped.
 * - [BrowserNavState.pendingSnapshotIgnore]: incremented before every browser-initiated navigator
 *   call so the resulting [snapshotFlow] emission doesn't try to re-sync the browser history.
 */
@Composable
actual fun BrowserHistoryEffect(navStack: NavStack<out NavStack.Record>, navigator: Navigator) {
  val state = remember { BrowserNavState(initialDepth = navStack.currentDepth()) }

  // Stamp the initial browser history entry with the current screen's real URL path
  LaunchedEffect(Unit) { replaceDepth(state.depth, currentUrl(navStack)) }

  // Circuit navStack changes → keep browser history in sync
  LaunchedEffect(navStack) {
    var prevSize = navStack.size
    snapshotFlow { navStack.size to navStack.currentDepth() }
      .drop(1) // skip initial emission representing the current state
      .distinctUntilChanged()
      .collect { (newSize, newDepth) ->
        val sizeDelta = newSize - prevSize
        val depthDelta = newDepth - state.depth
        // Always update tracking state first so subsequent emissions compute correct deltas
        prevSize = newSize
        state.depth = newDepth

        if (state.pendingSnapshotIgnore > 0) {
          // Change was driven by a browser event we already handled — don't re-sync
          state.pendingSnapshotIgnore--
          return@collect
        }

        when {
          sizeDelta > 0 -> {
            // App pushed new screen(s): add browser history entries using real URLs
            repeat(depthDelta) { _ -> pushDepth(newDepth, currentUrl(navStack)) }
          }
          sizeDelta < 0 -> {
            // App popped screen(s): walk browser history back by the depth change
            // (depthDelta is negative; use its magnitude via abs)
            val steps = -depthDelta
            state.pendingPopStateIgnore += steps
            window.history.go(-steps)
          }
          sizeDelta == 0 && depthDelta > 0 -> {
            // App called forward() — move to an existing record in forward history
            state.pendingPopStateIgnore++
            window.history.forward()
          }
          sizeDelta == 0 && depthDelta < 0 -> {
            // App called backward() — move to an existing record in back history
            val steps = -depthDelta
            state.pendingPopStateIgnore += steps
            window.history.go(-steps)
          }
        }
      }
  }

  // Browser back/forward buttons → Circuit navigator
  DisposableEffect(navigator) {
    val handler: (Event) -> Unit = {
      if (state.pendingPopStateIgnore > 0) {
        // popstate was triggered by our own history call — skip it
        state.pendingPopStateIgnore--
      } else {
        // Recover the depth index we stamped into history.state when we pushed this entry.
        val newDepth = historyDepth()
        val delta = newDepth - state.depth

        if (delta != 0) {
          // Increment before calling navigator: backward()/forward() mutate the Compose
          // snapshot synchronously, which would fire the snapshotFlow. The counter
          // prevents that emission from re-syncing the browser history.
          state.pendingSnapshotIgnore++
          val moved =
            when {
              delta < 0 -> navigator.backward()
              else -> navigator.forward()
            }
          // If navigator didn't move (e.g. no forward history exists), undo increment
          if (!moved) state.pendingSnapshotIgnore--
        }
      }
    }
    window.addEventListener("popstate", handler)
    onDispose { window.removeEventListener("popstate", handler) }
  }
}

/** Depth of the currently active record: 0 = root, 1 = one level in, etc. */
private fun NavStack<out NavStack.Record>.currentDepth(): Int =
  snapshot()?.backwardItems?.count() ?: 0

/**
 * The URL to show in the address bar for the currently active screen.
 *
 * Uses the screen's canonical path (e.g. `/recipe/52772`). Screens with no URL mapping keep the
 * current path so navigating in or out of them doesn't churn the address bar — the depth index that
 * distinguishes the history entries rides in `history.state`, not the URL.
 */
private fun currentUrl(navStack: NavStack<out NavStack.Record>): String =
  navStack.currentRecord?.screen?.toUrlPath() ?: window.location.pathname

/**
 * Pushes a new browser history entry for [url] with [depth] stored in `history.state`.
 *
 * Implemented per web target because kotlinx-browser types `history.state` as `Any?` on js and
 * `JsAny?` on wasmJs; the depth is stored as a string to round-trip cleanly on both.
 */
internal expect fun pushDepth(depth: Int, url: String)

/** Replaces the current browser history entry for [url] with [depth] stored in `history.state`. */
internal expect fun replaceDepth(depth: Int, url: String)

/** Reads the depth previously stored in `history.state`, or `0` if absent/unparseable. */
internal expect fun historyDepth(): Int

private class BrowserNavState(initialDepth: Int) {
  var depth: Int = initialDepth

  /**
   * Upcoming `popstate` events to silently ignore (we called [org.w3c.dom.History.go] / back /
   * forward).
   */
  var pendingPopStateIgnore: Int = 0

  /** Upcoming [snapshotFlow] emissions to silently ignore (browser initiated navigation). */
  var pendingSnapshotIgnore: Int = 0
}
