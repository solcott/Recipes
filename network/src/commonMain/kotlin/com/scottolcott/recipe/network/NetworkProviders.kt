package com.scottolcott.recipe.network

import com.scottolcott.recipe.config.RuntimeConfig
import com.scottolcott.recipe.serialization.NetworkJson
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.Qualifier
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger as KtorLogger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.resources.Resources
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

@Qualifier annotation class ApiClient

@Qualifier annotation class CoilClient

private const val MAX_RETRIES = 3

@ContributesTo(AppScope::class)
interface NetworkProviders {

  @Provides
  @NetworkJson
  @SingleIn(AppScope::class)
  fun provideNetworkJson(): Json = Json { ignoreUnknownKeys = true }

  @ApiClient
  @SingleIn(AppScope::class)
  @Provides
  fun provideKtorClient(
    logger: KtorLogger,
    runtimeConfig: RuntimeConfig,
    @NetworkJson json: Json,
  ): HttpClient {
    return HttpClient(provideKtorEngineFactory()) {
      expectSuccess = true
      install(Resources)
      install(DefaultRequest) {
        val baseUrl =
          if (runtimeConfig.mealDbApiKey != null) {
            "https://www.themealdb.com/api/json/v2/${runtimeConfig.mealDbApiKey}/"
          } else {
            "https://www.themealdb.com/api/json/v1/1/"
          }
        url(baseUrl)
      }
      install(HttpRequestRetry) { retryOnExceptionOrServerErrors(MAX_RETRIES) }

      install(ContentNegotiation) { json(json) }

      install(Logging) {
        this.logger = logger
        level = if (runtimeConfig.debugBuild) LogLevel.ALL else LogLevel.HEADERS
      }

      engine { configureEngine() }
    }
  }

  @CoilClient
  @SingleIn(AppScope::class)
  @Provides
  fun provideCoilKtorClient(logger: KtorLogger, runtimeConfig: RuntimeConfig): HttpClient {
    return HttpClient(provideKtorEngineFactory()) {
      expectSuccess = true
      install(HttpRequestRetry) { retryOnExceptionOrServerErrors(MAX_RETRIES) }
      engine { configureEngine() }
      install(Logging) {
        this.logger = logger
        level = LogLevel.INFO
      }
    }
  }
}

expect fun provideKtorEngineFactory(): HttpClientEngineFactory<*>

// Currently needed until https://github.com/touchlab/Kermit/issues/474 is fixed
expect fun HttpClientEngineConfig.configureEngine()
