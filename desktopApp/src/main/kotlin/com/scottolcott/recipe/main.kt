package com.scottolcott.recipe

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.scottolcott.recipe.domain.AppDesign
import com.scottolcott.recipe.domain.navigation.urlPathToScreen
import com.slack.circuit.runtime.screen.Screen
import dev.zacsweers.metro.createGraph

/**
 * Accepts an optional deep-link URL as the first command-line argument.
 *
 * Demo: ./gradlew :desktopApp:run --args="recipes://app/recipe/52772"
 */
fun main(args: Array<String>) {
  // Built here rather than inside `application`, whose content parameter is
  // `@Composable ApplicationScope.() -> Unit` -- a graph created in there is rebuilt by every
  // recomposition of the application root, taking the database connection and HTTP client with it.
  val graph = createGraph<DesktopAppGraph>()
  val initialScreen = args.firstOrNull()?.let { urlPathToScreen(it) }
  // See desktopApp/build.gradle.kts: `./gradlew :desktopApp:hotRun -Pdesign=cupertino` previews
  // the iOS design without a simulator.
  val design =
    if (System.getProperty("recipes.design").equals("cupertino", ignoreCase = true)) {
      AppDesign.Cupertino
    } else {
      AppDesign.Material
    }
  application { RecipeWindow(graph, initialScreen, design) }
}

@Composable
private fun ApplicationScope.RecipeWindow(
  graph: DesktopAppGraph,
  initialScreen: Screen?,
  design: AppDesign,
) {
  // The Cupertino preview opens phone-shaped, since that is the only size that shows the tab-bar
  // layout. `rememberWindowState` owns the size, so resizing the window from outside will not do.
  val size = if (design == AppDesign.Cupertino) DpSize(430.dp, 900.dp) else DpSize(1024.dp, 768.dp)
  val state = rememberWindowState(size = size, placement = WindowPlacement.Floating)
  // Desktop has no system back at all -- no predictive back, no swipe, no browser history -- so the
  // keyboard shortcuts are handled here, at the window, where they fire regardless of what holds
  // focus. Returning false leaves the key for whatever would have received it.
  val backShortcutHost = remember { BackShortcutHost() }
  Window(
    title = "Recipes",
    onCloseRequest = ::exitApplication,
    state = state,
    alwaysOnTop = false,
    onKeyEvent = { event -> isBackShortcut(event) && backShortcutHost.requestBack() },
  ) {
    RecipeApp(
      circuit = graph.circuit,
      subCircuit = graph.subCircuit,
      initialScreen = initialScreen,
      backShortcutHost = backShortcutHost,
      design = design,
    )
  }
}
