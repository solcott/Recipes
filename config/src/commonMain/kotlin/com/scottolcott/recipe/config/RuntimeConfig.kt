package com.scottolcott.recipe.config

interface RuntimeConfig {

  val debugBuild: Boolean
  val mealDbApiKey: String?
}
