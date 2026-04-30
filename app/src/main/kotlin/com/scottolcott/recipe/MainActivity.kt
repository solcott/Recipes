package com.scottolcott.recipe

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.slack.circuit.foundation.Circuit
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.android.ActivityKey

@ContributesIntoMap(AppScope::class, binding = binding<Activity>())
@ActivityKey
@Inject
class MainActivity(private val circuit: Circuit) : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RecipeApp(circuit, modifier = Modifier.fillMaxSize(), onRootPop = { finish() })
        }
    }
}
