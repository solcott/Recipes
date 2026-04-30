package com.scottolcott.recipe.ui.web

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeViewport
import com.scottolcott.recipe.RecipeApp
import dev.zacsweers.metro.createGraph

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
  try {
    val graph = createGraph<WebAppGraph>()
    ComposeViewport { RecipeApp(circuit = graph.circuit, modifier = Modifier.fillMaxSize()) }
  } catch (e: Throwable) {
    // Annoyingly this is the only way to get a useful stacktrace in the web console
    e.printStackTrace()
    throw e
  }
}
