package com.scottolcott.recipe

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import com.scottolcott.recipe.domain.navigation.urlPathToScreen
import dev.zacsweers.metro.createGraph

/** Standard entry point — used for normal app launch with no deep link. */
@Suppress("unused", "FunctionName")
fun MainViewController() = MainViewController(deepLinkUrl = null)

/**
 * Deep-link entry point called from Swift when the app is opened via a URL scheme.
 *
 * [deepLinkUrl] is the raw URL string (e.g. `"recipes://app/recipe/52772"`). Parsing happens here
 * so Swift never needs to know about [Screen] types.
 *
 * Demo: xcrun simctl openurl booted "recipes://app/recipe/52772"
 */
@Suppress("unused", "FunctionName")
fun MainViewController(deepLinkUrl: String?) = ComposeUIViewController {
  val appGraph = createGraph<IOSAppGraph>()
  val initialScreen = deepLinkUrl?.let { urlPathToScreen(it) }
  RecipeApp(appGraph.circuit, modifier = Modifier.fillMaxSize(), initialScreen = initialScreen)
}
