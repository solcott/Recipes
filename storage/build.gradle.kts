@file:OptIn(
  ExperimentalMetroGradleApi::class,
  org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class,
)

import dev.zacsweers.metro.gradle.ExperimentalMetroGradleApi

plugins {
  id("kmp.library")
  alias(libs.plugins.androidx.room)
  alias(libs.plugins.ksp)
  alias(libs.plugins.kotlinx.serialation)
  alias(libs.plugins.metro)
}

kotlin {
  jvm()

  sourceSets {
    commonMain {
      dependencies {
        api(libs.androidx.datastore.core)
        api(libs.androidx.room.runtime)
        api(libs.androidx.sqlite)
        api(libs.kotlin.serialization.core)
        api(libs.kotlinx.collections.immutable)
        api(libs.kotlinx.coroutines)
        api(projects.model)

        implementation(libs.androidx.collection)
        implementation(libs.androidx.datastore.core.okio)
        implementation(libs.kotlin.serialization.json)
        implementation(libs.okio)
      }
    }

    nonWebMain { dependencies { implementation(libs.androidx.sqlite.bundled) } }

    jvmMain { dependencies { api(projects.model) } }

    webMain {
      dependencies {
        implementation(libs.androidx.sqlite.web)
        implementation(projects.sqliteWasmWorker)
      }
    }
  }
}

dependencyAnalysis {
  issues {
    onUnusedDependencies {
      exclude(libs.androidx.sqlite.web.get().module.toString())
      exclude(projects.sqliteWasmWorker)
    }
  }
}

dependencies {
  kspAndroid(libs.androidx.room.compiler)

  kspIosArm64(libs.androidx.room.compiler)

  kspIosSimulatorArm64(libs.androidx.room.compiler)

  kspJs(libs.androidx.room.compiler)

  kspJvm(libs.androidx.room.compiler)

  kspWasmJs(libs.androidx.room.compiler)
}

room3 { schemaDirectory("$projectDir/schemas") }

metro { generateContributionProviders = true }
