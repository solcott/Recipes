@file:OptIn(ExperimentalMetroGradleApi::class)

import dev.zacsweers.metro.gradle.ExperimentalMetroGradleApi

plugins {
  id("kmp.library")
  alias(libs.plugins.metro)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.redacted)
  alias(libs.plugins.test.balloon)
  alias(libs.plugins.kotlinx.serialization)
}

kotlin {
  android { withHostTest { isReturnDefaultValues = true } }
  sourceSets {
    commonMain {
      dependencies {
        api(projects.model)
        api(libs.circuit.codegen.annotations)
        api(libs.circuit.foundation)
        api(libs.circuit.runtime)
        api(libs.circuit.runtime.navigation)
        api(libs.circuit.runtime.presenter)
        api(libs.circuit.runtime.screen)
        api(libs.circuit.runtime.ui)
        api(libs.circuit.serialization)
        api(libs.circuitx.subcircuit)
        // SearchState exposes TextFieldState. Circuit brings foundation in transitively but only
        // at 1.11.1, which loses to nothing in commonMain and downgrades the whole metadata
        // compilation below the Compose plugin version.
        api(libs.compose.foundation)
        api(libs.compose.material3)
        api(libs.kermit)
        api(libs.kotlinx.coroutines)
        // Used for url encode/decode in ScreenUrlMapper
        api(libs.ktor.http)
        api(libs.store)

        implementation(projects.core)
        implementation(projects.repository)
        implementation(libs.androidx.window.core)
        implementation(libs.kermit.core)
      }
    }

    commonTest {
      dependencies {
        implementation(libs.circuit.test)
        implementation(libs.kotlin.serialization.json)
        implementation(libs.kotlin.test)
        implementation(libs.kotlinx.coroutines.test)
        implementation(libs.test.balloon.framework.core)
        implementation(libs.turbine)
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
        api(projects.model)
        api(libs.androidx.compose.runtime.desktop)
        api(libs.circuit.codegen.annotations)
        api(libs.compose.foundation.desktop)

        implementation(projects.repository)
        implementation(libs.androidx.compose.runtime.saveable.desktop)
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
