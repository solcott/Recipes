package com.scottolcott.recipe.ui.web

import co.touchlab.kermit.Logger
import co.touchlab.kermit.NoTagFormatter
import co.touchlab.kermit.loggerConfigInit
import co.touchlab.kermit.platformLogWriter
import coil3.PlatformContext
import com.scottolcott.recipe.AppGraph
import com.scottolcott.recipe.config.RuntimeConfig
import com.scottolcott.recipe.di.ApplicationContext
import com.scottolcott.recipe.di.CoilProviders
import com.scottolcott.recipe.domain.circuit.CircuitProviders
import com.scottolcott.recipe.network.NetworkProviders
import com.scottolcott.recipe.storage.StorageProviders
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides

@DependencyGraph(
  AppScope::class,
  bindingContainers =
    [
      CoilProviders::class,
      CircuitProviders::class,
      StorageProviders::class,
      NetworkProviders::class,
    ],
)
interface WebAppGraph : AppGraph {

  @Provides
  @ApplicationContext
  fun providePlatformContext(): PlatformContext = PlatformContext.INSTANCE

  @Provides
  override fun provideLogger(): Logger =
    Logger(loggerConfigInit(platformLogWriter(NoTagFormatter)), "RecipeApp")

  @Provides
  override fun provideRuntimeConfig(): RuntimeConfig {
    return RuntimeConfigImpl()
  }
}
