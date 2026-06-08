package com.scottolcott.recipe

import kotlinx.browser.window

/**
 * js implementation of the `history.state` depth shims declared in `BrowserHistoryEffect.web.kt`.
 *
 * A separate actual is needed per web target because kotlinx-browser types the History API
 * differently on js and wasmJs: here `pushState`/`replaceState` accept `kotlin.Any?` and
 * `history.state` is a `kotlin.Any?`, so the depth string can be passed and read back as a plain
 * Kotlin [String]. The wasmJs target instead uses `kotlin.js.JsAny?` and requires bridging through
 * `JsString`. The shared `webMain` source set can't express both, so the read/write of
 * `history.state` is delegated to these per-target actuals.
 */
internal actual fun pushDepth(depth: Int, url: String) {
  window.history.pushState(depth.toString(), "", url)
}

internal actual fun replaceDepth(depth: Int, url: String) {
  window.history.replaceState(depth.toString(), "", url)
}

internal actual fun historyDepth(): Int = (window.history.state as? String)?.toIntOrNull() ?: 0
