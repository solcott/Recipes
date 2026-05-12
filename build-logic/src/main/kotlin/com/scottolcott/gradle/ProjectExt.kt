package com.scottolcott.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension

val Project.versionCatalog
  get() = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

val Project.mealDbApiKey: String?
  get() = (findProperty("MEALDB_API_KEY") as? String) ?: System.getenv("MEALDB_API_KEY")
