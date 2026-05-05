package com.scottolcott.recipe

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.window.core.layout.WindowSizeClass
import com.scottolcott.recipe.domain.presenter.RecipeScaffoldScreen
import com.scottolcott.recipe.ui.theme.RecipeAppTheme
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.CircuitContent
import com.slack.circuit.foundation.navstack.rememberSaveableNavStack
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen

@Composable expect fun rememberNavigator(navigator: Navigator): Navigator

@Suppress("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RecipeApp(
  circuit: Circuit,
  modifier: Modifier = Modifier,
  onRootPop: (result: PopResult?) -> Unit = {},
  windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass,
) {
  RecipeAppTheme {
    CompositionLocalProvider(LocalWindowSizeClass provides windowSizeClass) {
      CircuitCompositionLocals(circuit) {
        val initialBackstack = remember { listOf<Screen>(RecipeScaffoldScreen) }
        val backStack = rememberSaveableNavStack(initialBackstack)
        val circuitNavigator = rememberCircuitNavigator(backStack, onRootPop)
        val navigator = rememberNavigator(circuitNavigator)
        CircuitContent(RecipeScaffoldScreen, navigator = navigator, modifier = modifier)
      }
    }
  }
}

@Suppress("CompositionLocalAllowlist")
val LocalWindowSizeClass =
  compositionLocalOf<WindowSizeClass> { error("No WindowSizeClass provided") }
