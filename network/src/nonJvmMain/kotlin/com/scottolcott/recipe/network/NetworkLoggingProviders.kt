package com.scottolcott.recipe.network

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.ktor.KermitKtorLogger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.plugins.logging.Logger as KtorLogger

@ContributesTo(AppScope::class)
interface NetworkLoggingProviders {
  @Provides
  @SingleIn(AppScope::class)
  fun provideKtorLogger(logger: Logger): KtorLogger {
    return KermitKtorLogger(severity = Severity.Info, logger = logger)
  }
}
