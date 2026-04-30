package com.scottolcott.recipe

import android.app.Application
import android.content.Context
import co.touchlab.kermit.LogcatWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.loggerConfigInit
import com.scottolcott.recipe.di.ApplicationContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.android.MetroAppComponentProviders

@DependencyGraph(AppScope::class)
interface AndroidAppGraph : AppGraph, MetroAppComponentProviders {

  @Provides
  @ApplicationContext
  fun provideApplicationContext(application: Application): Context = application

  @Provides
  override fun provideLogger(): Logger = Logger(loggerConfigInit(LogcatWriter()), "RecipeApp")

  @DependencyGraph.Factory
  fun interface Factory {
    fun create(@Provides application: Application): AndroidAppGraph
  }
}