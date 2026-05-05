package com.scottolcott.recipe.network

import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.engine.darwin.DarwinClientEngineConfig

actual fun provideKtorEngineFactory(): HttpClientEngineFactory<*> {
  return Darwin
}

actual fun HttpClientEngineConfig.configureEngine() {
  this as DarwinClientEngineConfig
  configureRequest { setAllowsCellularAccess(true) }
}
