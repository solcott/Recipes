package com.scottolcott.recipe

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import com.scottolcott.recipe.domain.navigation.toUrlPath
import com.scottolcott.recipe.domain.presenter.HomeScreen
import com.scottolcott.recipe.domain.presenter.HomeTabScreen
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
 * The [snapshotFlow] observes [NavStack.size], [NavStack.currentDepth] and the root screen
 * together. Comparing them lets us distinguish every navigation type:
 *
 * | root    | sizeΔ | depthΔ | type                         | browser action   |
 * |---------|-------|--------|------------------------------|------------------|
 * | changed | any   | any    | root swap (rail section)     | rewind + replace |
 * | same    | > 0   | > 0    | push (goTo)                  | pushState        |
 * | same    | < 0   | < 0    | pop (truncates fwd history)  | history.go(δ)    |
 * | same    | 0     | > 0    | forward() — existing record  | history.forward  |
 * | same    | 0     | < 0    | backward() — existing record | history.back     |
 *
 * The root has to be watched in its own right: `RecipeScaffoldPresenter` swaps it when the
 * navigation rail changes section, and `[Home] → [Favorites]` moves neither the size nor the depth,
 * so the deltas alone would classify it as no navigation at all.
 *
 * ## Requires a NavStack, not a BackStack
 *
 * [Navigator.backward] and [Navigator.forward] delegate to the underlying stack, and `BackStack`
 * stubs both to `false`. Building this on a `rememberSaveableBackStack` leaves the browser's
 * back/forward buttons changing only the URL — [navStack] must come from
 * `rememberSaveableNavStack`.
 *
 * ## Counter semantics
 * - [BrowserNavState.pendingPopStateIgnore]: incremented once before every [org.w3c.dom.History.go]
 *   / [org.w3c.dom.History.back] / [org.w3c.dom.History.forward] call we make so the async
 *   `popstate` it fires is silently dropped. Once per *call*, not per entry crossed: a traversal
 *   spanning several entries still fires a single `popstate`, so counting the steps would strand
 *   the counter above zero and swallow the user's next back press.
 * - [BrowserNavState.pendingSnapshotIgnore]: incremented before every browser-initiated navigator
 *   call so the resulting [snapshotFlow] emission doesn't try to re-sync the browser history.
 * - [BrowserNavState.pendingRootUrl]: the URL a root swap still owes the depth-0 entry, stamped
 *   once the rewind it queued has finished. See [swapRoot].
 */
@Composable
actual fun BrowserHistoryEffect(navStack: NavStack<out NavStack.Record>, navigator: Navigator) {
  val state = remember { BrowserNavState(initialDepth = navStack.currentDepth()) }

  // Stamp the initial browser history entry with the current screen's real URL path
  LaunchedEffect(Unit) { replaceDepth(state.depth, currentUrl(navStack)) }

  NavStackToHistoryEffect(navStack, state)
  HistoryToNavStackEffect(navStack, navigator, state)
}

/** Circuit [navStack] changes -> keep browser history in sync. */
@Composable
private fun NavStackToHistoryEffect(
  navStack: NavStack<out NavStack.Record>,
  state: BrowserNavState,
) {
  LaunchedEffect(navStack) {
    var prevSize = navStack.size
    var prevRoot = navStack.rootRecord?.screen
    snapshotFlow { Triple(navStack.size, navStack.currentDepth(), navStack.rootRecord?.screen) }
      .drop(1) // skip initial emission representing the current state
      .distinctUntilChanged()
      .collect { (newSize, newDepth, newRoot) ->
        val sizeDelta = newSize - prevSize
        val depthDelta = newDepth - state.depth
        // A root swap has to rewind history by where we *were*, so capture that before it moves.
        val previousDepth = state.depth
        val rootChanged = newRoot != prevRoot
        // Always update tracking state first so subsequent emissions compute correct deltas
        prevSize = newSize
        prevRoot = newRoot
        state.depth = newDepth

        if (state.pendingSnapshotIgnore > 0) {
          // Change was driven by a browser event we already handled — don't re-sync
          state.pendingSnapshotIgnore--
          return@collect
        }

        when {
          rootChanged -> swapRoot(state, previousDepth, currentUrl(navStack))
          sizeDelta > 0 -> {
            // App pushed new screen(s): add browser history entries using real URLs
            repeat(depthDelta) { _ -> pushDepth(newDepth, currentUrl(navStack)) }
          }
          sizeDelta < 0 -> {
            // App popped screen(s): walk browser history back by the depth change. One
            // history.go covers the whole distance, however many records popUntil removed.
            state.pendingPopStateIgnore++
            window.history.go(depthDelta)
          }
          sizeDelta == 0 && depthDelta > 0 -> {
            // App called forward() — move to an existing record in forward history
            state.pendingPopStateIgnore++
            window.history.forward()
          }
          sizeDelta == 0 && depthDelta < 0 -> {
            // App called backward() — move to an existing record in back history
            state.pendingPopStateIgnore++
            window.history.go(depthDelta)
          }
        }
      }
  }
}

/** Browser back/forward buttons -> Circuit [navigator]. */
@Composable
private fun HistoryToNavStackEffect(
  navStack: NavStack<out NavStack.Record>,
  navigator: Navigator,
  state: BrowserNavState,
) {
  DisposableEffect(navigator) {
    val handler: (Event) -> Unit = {
      if (state.pendingPopStateIgnore > 0) {
        // popstate was triggered by our own history call — skip it
        state.pendingPopStateIgnore--
        // The last one settles a rewind, so the entry a root swap wants to restamp is now current.
        if (state.pendingPopStateIgnore == 0) {
          state.pendingRootUrl?.let { replaceDepth(0, it) }
          state.pendingRootUrl = null
        }
      } else {
        followBrowserNavigation(navStack, navigator, state)
      }
    }
    window.addEventListener("popstate", handler)
    onDispose { window.removeEventListener("popstate", handler) }
  }
}

/** Moves the [navigator] to wherever a browser-initiated `popstate` just landed. */
private fun followBrowserNavigation(
  navStack: NavStack<out NavStack.Record>,
  navigator: Navigator,
  state: BrowserNavState,
) {
  // Recover the depth index we stamped into history.state when we pushed this entry.
  val delta = historyDepth() - state.depth
  if (delta == 0) return

  // Increment before calling navigator: backward()/forward() mutate the Compose snapshot
  // synchronously, which would fire the snapshotFlow. The counter prevents that emission from
  // re-syncing the browser history.
  state.pendingSnapshotIgnore++
  val moved = if (delta < 0) navigator.backward() else navigator.forward()
  if (!moved) {
    // The navigator didn't move: the browser landed on an entry the stack has no record for, which
    // a root swap leaves behind because rewinding cannot truncate the forward entries. Undo the
    // increment and put the address bar back on the screen on show.
    state.pendingSnapshotIgnore--
    replaceDepth(state.depth, currentUrl(navStack))
  }
}

@Composable
actual fun BrowserTabUrlEffect(tab: HomeTabScreen) {
  // replaceState rather than pushState: switching tabs should not add a history entry, and the
  // depth is read back out of history.state so this shares no mutable state with BrowserNavState.
  LaunchedEffect(tab) { HomeScreen(tab).toUrlPath()?.let { replaceDepth(historyDepth(), it) } }
}

/**
 * Mirrors a root swap: rewind browser history to the depth-0 entry and restamp it with [url].
 *
 * Replacing rather than pushing is what keeps history depth mirroring stack depth -- the new root
 * sits at depth 0, so it has to occupy the entry already stamped 0. From depth 0 there is nothing
 * to rewind and the replace is immediate; deeper, [org.w3c.dom.History.go] is asynchronous and the
 * entry only becomes current once its `popstate` lands, so the URL is handed to the handler that
 * already drains [BrowserNavState.pendingPopStateIgnore].
 *
 * The old forward entries survive the rewind -- history cannot be truncated without pushing -- so a
 * browser-forward can still reach an entry the stack has no record for. The `popstate` handler
 * restamps the address bar when the navigator declines to move.
 */
private fun swapRoot(state: BrowserNavState, previousDepth: Int, url: String) {
  if (previousDepth == 0) {
    replaceDepth(0, url)
    return
  }
  state.pendingRootUrl = url
  state.pendingPopStateIgnore++
  window.history.go(-previousDepth)
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

  /** URL a root swap owes the depth-0 entry once its rewind settles; see [swapRoot]. */
  var pendingRootUrl: String? = null
}
