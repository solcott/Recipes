package com.scottolcott.recipe.ui.web

import co.touchlab.kermit.Logger
import co.touchlab.kermit.NoTagFormatter
import co.touchlab.kermit.loggerConfigInit
import co.touchlab.kermit.platformLogWriter
import coil3.PlatformContext
import com.scottolcott.recipe.AppGraph
import com.scottolcott.recipe.config.RuntimeConfig
import com.scottolcott.recipe.di.ApplicationContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import kotlinx.browser.window

@DependencyGraph(AppScope::class)
interface WebAppGraph : AppGraph {

  @Provides
  @ApplicationContext
  fun providePlatformContext(): PlatformContext = PlatformContext.INSTANCE

  @Provides
  override fun provideLogger(): Logger =
    Logger(loggerConfigInit(platformLogWriter(NoTagFormatter)), "RecipeApp")

  @Provides
  override fun provideRuntimeConfig(): RuntimeConfig {
    return object : RuntimeConfig {

      override val debugBuild: Boolean
        get() = window.location.hostname in listOf("localhost", "127.0.0.1", "[::1]")
    }
  }
}
