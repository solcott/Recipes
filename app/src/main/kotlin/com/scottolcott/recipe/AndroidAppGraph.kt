package com.scottolcott.recipe

import android.app.Application
import android.content.Context
import co.touchlab.kermit.LogcatWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.loggerConfigInit
import com.scottolcott.recipe.config.RuntimeConfig
import com.scottolcott.recipe.di.ApplicationContext
import com.scottolcott.recipe.di.CoilProviders
import com.scottolcott.recipe.domain.circuit.CircuitProviders
import com.scottolcott.recipe.network.NetworkProviders
import com.scottolcott.recipe.storage.StorageProviders
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.android.MetroAppComponentProviders

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
interface AndroidAppGraph : AppGraph, MetroAppComponentProviders {
  @Provides
  @ApplicationContext
  fun provideApplicationContext(application: Application): Context = application

  @Provides
  override fun provideLogger(): Logger = Logger(loggerConfigInit(LogcatWriter()), "RecipeApp")

  @Provides
  override fun provideRuntimeConfig(): RuntimeConfig {
    return RuntimeConfigImpl()
  }

  @DependencyGraph.Factory
  fun interface Factory {
    fun create(@Provides application: Application): AndroidAppGraph
  }
}
