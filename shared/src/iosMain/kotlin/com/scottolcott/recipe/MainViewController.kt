package com.scottolcott.recipe

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import dev.zacsweers.metro.createGraph

@Suppress("unused", "FunctionName")
fun MainViewController() = ComposeUIViewController {
  val appGraph = createGraph<IOSAppGraph>()
  RecipeApp(appGraph.circuit, modifier = Modifier.fillMaxSize())
}
