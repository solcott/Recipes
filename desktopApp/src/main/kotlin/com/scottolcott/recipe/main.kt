package com.scottolcott.recipe

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.zacsweers.metro.createGraph

fun main() = application {
  val graph = createGraph<DesktopAppGraph>()
  val state =
    rememberWindowState(size = DpSize(1024.dp, 768.dp), placement = WindowPlacement.Floating)
  Window(
    title = "Recipes",
    onCloseRequest = ::exitApplication,
    state = state,
    alwaysOnTop = false,
  ) {
    RecipeApp(graph.circuit)
  }
}
