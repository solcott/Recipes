package com.scottolcott.recipe.ui.web

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeViewport
import com.scottolcott.recipe.RecipeApp
import com.scottolcott.recipe.domain.navigation.urlPathToScreen
import dev.zacsweers.metro.createGraph
import kotlinx.browser.window

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
  val graph = createGraph<WebAppGraph>()
  // Parse the initial URL so the app opens at the right screen on direct load
  // (e.g. someone opens /recipe/52772 directly — no flash, no redirect needed).
  val initialScreen = urlPathToScreen(window.location.pathname)
  ComposeViewport {
    RecipeApp(
      circuit = graph.circuit,
      modifier = Modifier.fillMaxSize(),
      initialScreen = initialScreen,
    )
  }
}
