package com.scottolcott.recipe

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import dev.zacsweers.metro.createGraphFactory
import dev.zacsweers.metrox.android.MetroAppComponentProviders
import dev.zacsweers.metrox.android.MetroApplication

class RecipeApplication : Application(), MetroApplication, SingletonImageLoader.Factory {

    private val appGraph: AndroidAppGraph by lazy { createGraphFactory<AndroidAppGraph.Factory>().create(this) }
    override val appComponentProviders: MetroAppComponentProviders
        get() = appGraph

    override fun newImageLoader(context: PlatformContext): ImageLoader = appGraph.imageLoader

}