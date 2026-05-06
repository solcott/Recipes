package com.scottolcott.recipe

import com.scottolcott.recipe.config.RuntimeConfig

class RuntimeConfigImpl : RuntimeConfig {

  override val debugBuild: Boolean by lazy { System.getProperty("debug")?.toBoolean() ?: false }

  override val mealDbApiKey: String? = SharedBuildConfig.MEALDB_API_KEY.takeIf { it.isNotEmpty() }
}
