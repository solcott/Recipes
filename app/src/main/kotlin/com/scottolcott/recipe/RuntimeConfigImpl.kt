package com.scottolcott.recipe

import com.scottolcott.recipe.config.RuntimeConfig

class RuntimeConfigImpl : RuntimeConfig {

  override val debugBuild: Boolean = BuildConfig.DEBUG

  override val mealDbApiKey: String = SharedBuildConfig.MEALDB_API_KEY
}
