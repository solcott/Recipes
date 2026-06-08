package com.scottolcott.recipe

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.scottolcott.recipe.domain.navigation.urlPathToScreen
import dev.zacsweers.metro.createGraph

/**
 * Accepts an optional deep-link URL as the first command-line argument.
 *
 * Demo: ./gradlew :desktopApp:run --args="recipes://app/recipe/52772"
 */
fun main(args: Array<String>) = application {
  val graph = createGraph<DesktopAppGraph>()
  val initialScreen = args.firstOrNull()?.let { urlPathToScreen(it) }
  val state =
    rememberWindowState(size = DpSize(1024.dp, 768.dp), placement = WindowPlacement.Floating)
  Window(
    title = "Recipes",
    onCloseRequest = ::exitApplication,
    state = state,
    alwaysOnTop = false,
  ) {
    RecipeApp(graph.circuit, initialScreen = initialScreen)
  }
}
