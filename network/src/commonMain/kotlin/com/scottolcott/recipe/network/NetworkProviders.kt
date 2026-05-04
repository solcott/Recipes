package com.scottolcott.recipe.network

import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.Qualifier
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.resources.Resources
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

@Qualifier annotation class ApiClient

@Qualifier annotation class CoilClient

@ContributesTo(AppScope::class)
interface NetworkProviders {

  @ApiClient
  @SingleIn(AppScope::class)
  @Provides
  fun provideKtorClient(logger: Logger): HttpClient {
    return HttpClient(provideKtorEngineFactory()) {
      expectSuccess = true
      install(Resources)
      install(DefaultRequest) { url("https://www.themealdb.com/api/json/v1/1/") }
      install(HttpRequestRetry) { retryOnExceptionOrServerErrors(3) }

      install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }

      installPlatformSpecificKtorPlugins(logger)
    }
  }

  @CoilClient
  @SingleIn(AppScope::class)
  @Provides
  fun provideCoilKtorClient(logger: Logger): HttpClient {
    return HttpClient(provideKtorEngineFactory()) {
      expectSuccess = true
      install(HttpRequestRetry) { retryOnExceptionOrServerErrors(3) }

      installPlatformSpecificKtorPlugins(logger)
    }
  }
}

expect fun provideKtorEngineFactory(): HttpClientEngineFactory<*>

// Currently needed until https://github.com/touchlab/Kermit/issues/474 is fixed
expect fun HttpClientConfig<out HttpClientEngineConfig>.installPlatformSpecificKtorPlugins(
  logger: Logger
)
