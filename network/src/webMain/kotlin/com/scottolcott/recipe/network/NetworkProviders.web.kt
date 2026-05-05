package com.scottolcott.recipe.network

import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.js.Js

actual fun provideKtorEngineFactory(): HttpClientEngineFactory<*> = Js

actual fun HttpClientEngineConfig.configureEngine() {
  // no-op
}
