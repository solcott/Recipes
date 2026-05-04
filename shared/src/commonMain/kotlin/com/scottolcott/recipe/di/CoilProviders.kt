package com.scottolcott.recipe.di

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.annotation.ExperimentalCoilApi
import coil3.disk.DiskCache
import coil3.network.CacheStrategy
import coil3.network.NetworkFetcher
import coil3.network.ktor3.asNetworkClient
import coil3.request.crossfade
import com.scottolcott.recipe.network.CoilClient
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient

internal const val MAX_CACHE_SIZE = 1024L * 1024L * 100L // 100 MB

@ContributesTo(AppScope::class)
interface CoilProviders {

  @OptIn(ExperimentalCoilApi::class)
  @SingleIn(AppScope::class)
  @Provides
  fun provideNetworkFactory(@CoilClient httpClient: HttpClient): NetworkFetcher.Factory {
    return NetworkFetcher.Factory({ httpClient.asNetworkClient() }, CacheStrategy::DEFAULT)
  }

  @OptIn(ExperimentalCoilApi::class)
  @SingleIn(AppScope::class)
  @Provides
  fun provideImageLoader(
    @ApplicationContext platformContext: PlatformContext,
    networkFetcher: NetworkFetcher.Factory,
  ): ImageLoader =
    ImageLoader.Builder(platformContext)
      .diskCache { newDiskCache() }
      .crossfade(true)
      // Disable noisy logging
      //            .logger(null)
      .components { add(networkFetcher) }
      .build()
}

expect fun newDiskCache(): DiskCache?
