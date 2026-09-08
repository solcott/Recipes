package com.scottolcott.recipe.network

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.ktor.KermitKtorLogger
import com.scottolcott.recipe.config.RuntimeConfig
import io.ktor.client.plugins.logging.Logger as KtorLogger

actual fun provideKtorLogger(logger: Logger, runtimeConfig: RuntimeConfig): KtorLogger {
  val severity = if (runtimeConfig.debugBuild) Severity.Debug else Severity.Info
  return KermitKtorLogger(severity = severity, logger = logger)
}
