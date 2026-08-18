@file:OptIn(ExperimentalMetroGradleApi::class)

import dev.zacsweers.metro.gradle.ExperimentalMetroGradleApi

plugins {
  id("kmp.library")
  alias(libs.plugins.metro)
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(projects.model)
        api(libs.kermit)
        api(libs.kotlinx.coroutines)
        api(libs.store)

        implementation(projects.network)
        implementation(projects.storage)
        implementation(libs.kermit.core)
      }
    }

    jvmMain {
      dependencies {
        api(projects.model)

        implementation(projects.network)
        implementation(projects.storage)
      }
    }
  }
}

metro { generateContributionProviders = true }

dependencyAnalysis {
  issues {
    onIncorrectConfiguration {
      exclude(projects.network)
      exclude(projects.storage)
    }
  }
}
