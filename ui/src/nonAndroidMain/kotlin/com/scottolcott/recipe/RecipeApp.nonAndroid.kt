package com.scottolcott.recipe

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.slack.circuit.runtime.Navigator

@Composable
actual fun rememberNavigator(navigator: Navigator): Navigator {
  return remember(navigator) { navigator }
}
