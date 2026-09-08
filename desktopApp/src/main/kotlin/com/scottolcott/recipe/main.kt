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
import com.scottolcott.recipe.domain.AppInput
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
  // See desktopApp/build.gradle.kts: `-Pdesign=cupertino` previews the iOS design without a
  // simulator, and `-Pinput=touch` drops the pointer density that desktop otherwise takes. The two
  // are independent on purpose -- `-Pdesign=cupertino -Pinput=touch` is what an iPhone actually
  // gets, and either one alone is a design nothing ships, which is exactly what makes them useful
  // for telling apart what each axis is doing.
  val design = systemPropertyOr("recipes.design", AppDesign.Material)
  val input = systemPropertyOr("recipes.input", AppInput.Pointer)
  application { RecipeWindow(graph, initialScreen, design, input) }
}

/**
 * Reads an enum out of a system property by name, falling back to [default] for anything the enum
 * does not know -- including the property being absent, which is how a plain `:desktopApp:run`
 * arrives here.
 */
private inline fun <reified T : Enum<T>> systemPropertyOr(property: String, default: T): T {
  val value = System.getProperty(property) ?: return default
  return enumValues<T>().firstOrNull { it.name.equals(value, ignoreCase = true) } ?: default
}

@Composable
private fun ApplicationScope.RecipeWindow(
  graph: DesktopAppGraph,
  initialScreen: Screen?,
  design: AppDesign,
  input: AppInput,
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
      input = input,
    )
  }
}
