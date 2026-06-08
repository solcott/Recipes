@file:OptIn(ExperimentalWasmJsInterop::class)

package com.scottolcott.recipe

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsString
import kotlin.js.toJsString
import kotlinx.browser.window

/**
 * wasmJs implementation of the `history.state` depth shims declared in
 * `BrowserHistoryEffect.web.kt`.
 *
 * A separate actual is needed per web target because kotlinx-browser types the History API
 * differently on js and wasmJs: here `pushState`/`replaceState` accept `kotlin.js.JsAny?` and
 * `history.state` is a `JsAny?`, so the depth string must be bridged across the Wasm/JS boundary via
 * [toJsString] when writing and cast back through [JsString] when reading. The js target instead
 * uses plain `kotlin.Any?` and needs no bridging. The shared `webMain` source set can't express
 * both, so the read/write of `history.state` is delegated to these per-target actuals.
 */
internal actual fun pushDepth(depth: Int, url: String) {
  window.history.pushState(depth.toString().toJsString(), "", url)
}

internal actual fun replaceDepth(depth: Int, url: String) {
  window.history.replaceState(depth.toString().toJsString(), "", url)
}

internal actual fun historyDepth(): Int =
  (window.history.state as? JsString)?.toString()?.toIntOrNull() ?: 0
