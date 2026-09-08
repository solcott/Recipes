package com.scottolcott.recipe.ui.web

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeViewport
import com.scottolcott.recipe.RecipeApp
import com.scottolcott.recipe.domain.AppInput
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
      subCircuit = graph.subCircuit,
      modifier = Modifier.fillMaxSize(),
      initialScreen = initialScreen,
      input = webInput(),
    )
  }
}

/**
 * Which input the browser is being driven with.
 *
 * `RecipeApp` defaults the web to [AppInput.Pointer], which is right for a desktop browser and
 * wrong for the same page open on a tablet -- both are `AppPlatform.Web`, and only the browser can
 * tell them apart. `pointer: coarse` is the media query that asks, so the platform-to-design
 * mapping stays here at the entry point, where the `isIos()` one already lives.
 *
 * Read once at startup rather than watched: the design is fixed for the life of the composition,
 * and a hybrid laptop switching between trackpad and touchscreen mid-session is not worth
 * re-theming the whole app for.
 */
private fun webInput(): AppInput =
  if (window.matchMedia("(pointer: coarse)").matches) AppInput.Touch else AppInput.Pointer
