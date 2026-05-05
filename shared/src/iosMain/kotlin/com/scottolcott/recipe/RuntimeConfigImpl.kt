package com.scottolcott.recipe

import com.scottolcott.recipe.config.RuntimeConfig
import kotlin.experimental.ExperimentalNativeApi

class RuntimeConfigImpl : RuntimeConfig {
  @OptIn(ExperimentalNativeApi::class)
  override val debugBuild: Boolean by lazy { Platform.isDebugBinary }
}
