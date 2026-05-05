package com.scottolcott.recipe.ui.web

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeViewport
import com.scottolcott.recipe.RecipeApp
import dev.zacsweers.metro.createGraph

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
  val graph = createGraph<WebAppGraph>()
  ComposeViewport { RecipeApp(circuit = graph.circuit, modifier = Modifier.fillMaxSize()) }
}
