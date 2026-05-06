package com.scottolcott.recipe.network

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import com.scottolcott.recipe.config.RuntimeConfig
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.plugins.logging.Logger as KtorLogger

actual fun HttpClientEngineConfig.configureEngine() {
  // no-op
}

@ContributesTo(AppScope::class)
interface NetworkLoggingProviders {
  @Provides
  @SingleIn(AppScope::class)
  fun provideKtorLogger(logger: Logger, runtimeConfig: RuntimeConfig): KtorLogger {
    val severity = if (runtimeConfig.debugBuild) Severity.Debug else Severity.Info
    return KermitKtorLogger(severity = severity, logger = logger)
  }
}
