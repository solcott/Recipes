package com.scottolcott.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension

val Project.versionCatalog
  get() = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

/**
 * TheMealDB API key, required. The app talks to the v2 API exclusively, whose base URL embeds the
 * key in the path; there is no keyless fallback.
 */
val Project.mealDbApiKey: String
  get() =
    (findProperty("MEALDB_API_KEY") as? String)?.takeIf { it.isNotBlank() }
      ?: System.getenv("MEALDB_API_KEY")?.takeIf { it.isNotBlank() }
      ?: error(
        """
        MEALDB_API_KEY is required — the free v1 API fallback has been removed.
        Add it to ~/.gradle/gradle.properties or export MEALDB_API_KEY.
        Get a key: https://www.themealdb.com/api.php
        """
          .trimIndent()
      )
