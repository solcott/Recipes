package com.scottolcott.recipe

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import com.scottolcott.recipe.domain.navigation.urlPathToScreen
import dev.zacsweers.metro.createGraph
import platform.UIKit.UIViewController

/**
 * The Metro graph, held for the life of the process rather than the life of a composition.
 *
 * Two things make this the right scope. A graph built inside `ComposeUIViewController`'s content
 * lambda is rebuilt by every recomposition of that root, taking the Room connection, the Ktor
 * client, and every Store down with it. And `RecipesApp.swift` deliberately forces a whole new
 * `UIViewController` on each incoming URL via `.id(deepLinkUrl)`, so even a `remember` inside the
 * composition would be discarded once per deep link.
 *
 * File scope survives both, which is the same lifetime Android gets by scoping the graph to the
 * Application and injecting `MainActivity`, and web gets by building it in `main()` outside
 * `ComposeViewport`.
 *
 * `by lazy` defaults to `SYNCHRONIZED`, and the only callers below run on the main thread.
 */
private val appGraph: IOSAppGraph by lazy { createGraph<IOSAppGraph>() }

/** Standard entry point -- used for normal app launch with no deep link. */
@Suppress("unused", "FunctionName")
fun MainViewController(): UIViewController = MainViewController(deepLinkUrl = null)

/**
 * Deep-link entry point called from Swift when the app is opened via a URL scheme.
 *
 * [deepLinkUrl] is the raw URL string (e.g. `"recipes://app/recipe/52772"`). Parsing happens here
 * so Swift never needs to know about [Screen] types, and outside the content lambda so it does not
 * re-parse on every recomposition.
 *
 * Demo: xcrun simctl openurl booted "recipes://app/recipe/52772"
 */
@Suppress("unused", "FunctionName")
fun MainViewController(deepLinkUrl: String?): UIViewController {
  val initialScreen = deepLinkUrl?.let { urlPathToScreen(it) }
  return ComposeUIViewController {
    RecipeApp(
      circuit = appGraph.circuit,
      subCircuit = appGraph.subCircuit,
      modifier = Modifier.fillMaxSize(),
      initialScreen = initialScreen,
    )
  }
}
