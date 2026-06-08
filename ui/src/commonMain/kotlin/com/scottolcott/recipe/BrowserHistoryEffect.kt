package com.scottolcott.recipe

import androidx.compose.runtime.Composable
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.navigation.NavStack

/**
 * Synchronises Circuit's inner navigation stack with the browser's History API so that the browser
 * back/forward buttons drive Circuit navigation on web targets.
 *
 * On non-web platforms this is a no-op.
 */
@Composable
expect fun BrowserHistoryEffect(navStack: NavStack<out NavStack.Record>, navigator: Navigator)
