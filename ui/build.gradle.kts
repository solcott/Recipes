@file:OptIn(ExperimentalMetroGradleApi::class)

import dev.zacsweers.metro.gradle.ExperimentalMetroGradleApi

plugins {
  id("kmp.library")
  alias(libs.plugins.metro)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.kotlin.compose)
}

compose.resources {
  publicResClass = true
  packageOfResClass = "com.scottolcott.recipe.ui"
  generateResClass = always
}

kotlin {
  android { androidResources { enable = true } }

  sourceSets {
    commonMain {
      dependencies {
        api(libs.circuit.codegen.annotations)
        api(libs.circuit.foundation)
        api(libs.circuit.runtime)
        api(libs.circuit.runtime.screen)
        api(libs.circuit.runtime.ui)
        api(libs.compose.components.resources)
        api(libs.kotlinx.coroutines)
        api(projects.domain)
        api(projects.model)

        implementation(libs.circuit.backstack)
        implementation(libs.circuit.retained)
        implementation(libs.circuit.runtime.navigation)
        implementation(libs.circuit.sharedelements)
        implementation(libs.circuitx.gesture.navigation)
        implementation(libs.coil)
        implementation(libs.coil.compose)
        implementation(libs.coil.core)
        implementation(libs.coil.network.ktor)
        implementation(libs.compose.material3)
        implementation(libs.compose.material3.adaptive)
        implementation(libs.compose.ui.tooling.preview)
      }
    }

    androidMain {
      dependencies {
        api(libs.androidx.compose.foundation)
        api(libs.androidx.compose.foundation.layout)
        api(libs.androidx.compose.material3)
        api(libs.androidx.compose.runtime)
        api(libs.androidx.compose.ui)
        api(libs.androidx.compose.ui.text)
        api(libs.androidx.window.core)
        api(libs.circuit.codegen.annotations)

        implementation(libs.androidx.activity.compose)
        implementation(libs.androidx.compose.animation)
        implementation(libs.androidx.compose.animation.core)
        implementation(libs.androidx.compose.material3.adaptive)
        implementation(libs.androidx.compose.ui.graphics)
        implementation(libs.androidx.compose.ui.unit)
        implementation(libs.circuitx.android)
      }
    }

    jvmMain {
      dependencies {
        api(libs.androidx.compose.runtime.desktop)
        api(libs.androidx.window.core)
        api(libs.circuit.codegen.annotations)
        api(libs.compose.components.resources.desktop)
        api(libs.compose.foundation.desktop)
        api(libs.compose.foundation.layout.desktop)
        api(libs.compose.material3.desktop)
        api(libs.compose.ui.desktop)
        api(libs.compose.ui.text.desktop)
        api(projects.domain)
        api(projects.model)

        implementation(libs.compose.animation.core.desktop)
        implementation(libs.compose.animation.desktop)
        implementation(libs.compose.material3.adaptive.desktop)
        implementation(libs.compose.ui.graphics.desktop)
        implementation(libs.compose.ui.unit.desktop)
      }
    }

    webMain {
      dependencies {
        val wsDependency = npm("ws", libs.versions.ws.get())
        api(wsDependency)
      }
    }
  }
}

metro { enableCircuitCodegen = true }

dependencyAnalysis {
  issues {
    onUnusedDependencies {
      exclude(libs.compose.material3)
      exclude(libs.compose.material3.adaptive)
      exclude(libs.compose.ui.tooling.preview)
      exclude("org.jetbrains.compose.desktop:desktop-jvm-macos-arm64")
      exclude("org.jetbrains.compose.hot-reload:hot-reload-runtime-api")
    }
    onIncorrectConfiguration {}
  }
}
