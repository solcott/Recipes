package com.scottolcott.recipe

import co.touchlab.kermit.Logger
import coil3.ImageLoader
import com.scottolcott.recipe.config.RuntimeConfig
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.subcircuit.SubCircuit

interface AppGraph {

  val imageLoader: ImageLoader

  val circuit: Circuit

  val subCircuit: SubCircuit

  fun provideLogger(): Logger

  fun provideRuntimeConfig(): RuntimeConfig
}
