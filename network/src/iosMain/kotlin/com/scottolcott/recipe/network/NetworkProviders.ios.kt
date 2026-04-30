package com.scottolcott.recipe.network

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.ktor.KermitKtorLogger
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.engine.darwin.DarwinClientEngineConfig
import io.ktor.client.plugins.logging.Logging

actual fun provideKtorEngineFactory(): HttpClientEngineFactory<*> {
  return Darwin
}

actual fun HttpClientConfig<out HttpClientEngineConfig>.installPlatformSpecificKtorPlugins(
  logger: Logger
) {
  this.engine {}
  engine {
    this as DarwinClientEngineConfig
    configureRequest { setAllowsCellularAccess(true) }
  }
  install(Logging) { this.logger = KermitKtorLogger(severity = Severity.Info, logger = logger) }
}
