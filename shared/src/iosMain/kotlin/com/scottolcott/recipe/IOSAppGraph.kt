package com.scottolcott.recipe

import co.touchlab.kermit.Logger
import co.touchlab.kermit.NoTagFormatter
import co.touchlab.kermit.loggerConfigInit
import co.touchlab.kermit.platformLogWriter
import coil3.PlatformContext
import com.scottolcott.recipe.di.ApplicationContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides

@DependencyGraph(AppScope::class)
interface IOSAppGraph : AppGraph {

  @Provides
  @ApplicationContext
  fun providePlatformContext(): PlatformContext = PlatformContext.INSTANCE

  @Provides
  override fun provideLogger(): Logger =
    Logger(loggerConfigInit(platformLogWriter(NoTagFormatter)), "RecipeApp")
}
