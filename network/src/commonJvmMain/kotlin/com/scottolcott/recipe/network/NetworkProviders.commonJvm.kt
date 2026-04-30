package com.scottolcott.recipe.network

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp

actual fun provideKtorEngineFactory(): HttpClientEngineFactory<*> {
  return OkHttp
}
