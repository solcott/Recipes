@file:OptIn(ExperimentalMetroGradleApi::class)

import dev.zacsweers.metro.gradle.ExperimentalMetroGradleApi

plugins {
  id("kmp.library")
  alias(libs.plugins.metro)
  alias(libs.plugins.test.balloon)
}

kotlin {
  android { withHostTest { isReturnDefaultValues = true } }
  sourceSets {
    commonMain {
      dependencies {
        api(projects.model)
        // Repositories speak Outcome now, so `dataresult` is part of their public signatures.
        api(libs.dataresult)
        api(libs.kermit)
        api(libs.kotlinx.coroutines)

        implementation(projects.network)
        implementation(projects.storage)
        // `implementation`, not `api`: Store5 stopped being part of this module's public API the
        // moment repositories started returning Flow<Outcome<T>>. It stays an implementation
        // detail here, which is the point of the change -- :domain no longer compiles against it.
        implementation(libs.dataresultStore5)
        implementation(libs.kermit.core)
        implementation(libs.store)
      }
    }

    commonTest {
      dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.kotlinx.coroutines.test)
        implementation(libs.test.balloon.framework.core)
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
