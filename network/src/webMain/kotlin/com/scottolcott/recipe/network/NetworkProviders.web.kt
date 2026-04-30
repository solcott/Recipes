package com.scottolcott.recipe.network

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.ktor.KermitKtorLogger
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.logging.Logging

actual fun provideKtorEngineFactory(): HttpClientEngineFactory<*> = Js

actual fun HttpClientConfig<*>.installPlatformSpecificKtorPlugins(logger: Logger) {
  install(Logging) { this.logger = KermitKtorLogger(severity = Severity.Info, logger = logger) }
}
