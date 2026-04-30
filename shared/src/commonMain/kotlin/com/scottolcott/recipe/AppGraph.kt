package com.scottolcott.recipe

import co.touchlab.kermit.Logger
import coil3.ImageLoader
import com.slack.circuit.foundation.Circuit

interface AppGraph {

  val imageLoader: ImageLoader

  val circuit: Circuit

  fun provideLogger(): Logger
}
