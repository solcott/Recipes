package com.scottolcott.recipe.network

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClientConfig

actual fun HttpClientConfig<*>.installPlatformSpecificKtorPlugins(logger: Logger) {
  //    install(Logging) { this.logger = KermitKtorLogger(severity = Severity.Info, logger = logger)
  // }
}
