package com.scottolcott.recipe

import com.scottolcott.recipe.config.RuntimeConfig

class RuntimeConfigImpl : RuntimeConfig {

  override val debugBuild: Boolean by lazy {
    // FIXME This doesn't work
    val location =
      DesktopAppGraph::class.java.protectionDomain?.codeSource?.location?.path.orEmpty()
    location.isNotEmpty() && !location.endsWith(".jar")
  }
}
