package com.scottolcott.recipe

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import com.slack.circuit.runtime.Navigator
import com.slack.circuitx.android.rememberAndroidScreenAwareNavigator

@Composable
actual fun rememberNavigator(navigator: Navigator): Navigator {
    val activity = checkNotNull(LocalActivity.current) {
        "Activity is null"
    }
    return rememberAndroidScreenAwareNavigator(navigator, activity)
}
