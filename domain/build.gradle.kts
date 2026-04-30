@file:OptIn(ExperimentalMetroGradleApi::class)

import dev.zacsweers.metro.gradle.ExperimentalMetroGradleApi

plugins {
  id("kmp.library")
  alias(libs.plugins.metro)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.redacted)
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(libs.circuit.codegen.annotations)
        api(libs.circuit.foundation)
        api(libs.circuit.runtime)
        api(libs.circuit.runtime.navigation)
        api(libs.circuit.runtime.presenter)
        api(libs.circuit.runtime.screen)
        api(libs.circuit.runtime.ui)
        api(libs.kermit)
        api(libs.kotlinx.coroutines)
        api(libs.store)
        api(projects.model)

        implementation(libs.circuit.retained)
        implementation(libs.kermit.core)
        implementation(projects.repository)
      }
    }

    androidMain {
      dependencies {
        api(libs.androidx.compose.foundation)
        api(libs.androidx.compose.runtime)
        api(libs.circuit.codegen.annotations)

        implementation(libs.androidx.compose.runtime.saveable)
      }
    }
    jvmMain {
      dependencies {
        api(libs.androidx.compose.runtime.desktop)
        api(libs.circuit.codegen.annotations)
        api(libs.compose.foundation.desktop)
        api(projects.model)

        implementation(libs.androidx.compose.runtime.saveable.desktop)
        implementation(projects.repository)
      }
    }

    webMain {
      dependencies {
        implementation(libs.compose.foundation)
        implementation(libs.compose.ui)
      }
    }
  }
}

dependencyAnalysis {
  issues {
    onUnusedDependencies {
      exclude(libs.compose.ui)
      exclude(libs.compose.foundation)
      exclude("org.jetbrains.compose.desktop:desktop-jvm-macos-arm64")
      exclude("org.jetbrains.compose.hot-reload:hot-reload-runtime-api")
    }
    onIncorrectConfiguration { exclude(projects.repository) }
  }
}

metro { enableCircuitCodegen = true }
