package com.scottolcott.recipe

import com.scottolcott.recipe.config.RuntimeConfig

class RuntimeConfigImpl : RuntimeConfig {

  override val debugBuild: Boolean = com.scottolcott.recipe.BuildConfig.DEBUG

  override val mealDbApiKey: String? = SharedBuildConfig.MEALDB_API_KEY.takeIf { it.isNotEmpty() }
}
