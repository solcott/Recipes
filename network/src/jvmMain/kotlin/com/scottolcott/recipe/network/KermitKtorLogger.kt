package com.scottolcott.recipe.network

import co.touchlab.kermit.Logger as KermitLogger
import co.touchlab.kermit.LoggerConfig
import co.touchlab.kermit.Severity
import io.ktor.client.plugins.logging.Logger as KtorLogger

/** Required until Kermit > 2.1.0 is released */
class KermitKtorLogger(private val severity: Severity, private val logger: KermitLogger) :
  KtorLogger {

  constructor(
    severity: Severity,
    config: LoggerConfig,
    tag: String = "",
  ) : this(logger = KermitLogger(config = config, tag = tag), severity = severity)

  override fun log(message: String) {
    logger.log(severity, logger.tag, null, message)
  }
}
