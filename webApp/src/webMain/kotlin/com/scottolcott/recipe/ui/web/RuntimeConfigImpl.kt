package com.scottolcott.recipe.ui.web

import com.scottolcott.recipe.config.RuntimeConfig
import kotlinx.browser.window

class RuntimeConfigImpl : RuntimeConfig {

  override val debugBuild: Boolean
    get() = window.location.hostname in listOf("localhost", "127.0.0.1", "[::1]")
}
