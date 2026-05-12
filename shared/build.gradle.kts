@file:OptIn(ExperimentalMetroGradleApi::class)

import com.codingfeline.buildkonfig.compiler.FieldSpec
import com.scottolcott.gradle.mealDbApiKey
import dev.zacsweers.metro.gradle.ExperimentalMetroGradleApi

plugins {
  id("kmp.library")
  alias(libs.plugins.metro)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.buildkonfig)
}

buildkonfig {
  packageName = "com.scottolcott.recipe"
  exposeObjectWithName = "SharedBuildConfig"
  defaultConfigs {
    buildConfigField(FieldSpec.Type.STRING, "MEALDB_API_KEY", project.mealDbApiKey ?: "")
  }
}

compose.resources {
  publicResClass = true
  packageOfResClass = "com.scottolcott.recipe.shared"
  generateResClass = always
}

kotlin {
  android { androidResources { enable = true } }

  listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
    iosTarget.binaries.framework {
      baseName = "RecipeApp"
      isStatic = true
    }
  }

  sourceSets {
    commonMain {
      dependencies {
        api(libs.circuit.foundation)
        api(libs.circuit.runtime.presenter)
        api(libs.kotlinx.coroutines)
        api(projects.config)
        api(projects.domain)
        api(projects.model)
        api(projects.network)
        api(projects.repository)
        api(projects.storage)
        api(projects.ui)

        implementation(libs.circuit.codegen.annotations)
        implementation(libs.circuit.runtime.ui)
        implementation(libs.circuitx.gesture.navigation)
        implementation(libs.coil)
        implementation(libs.coil.compose)
        implementation(libs.coil.network.ktor)
        implementation(libs.compose.components.resources)
        implementation(libs.compose.material3)
        implementation(libs.compose.material3.adaptive)
        implementation(libs.compose.material3.window.size)
        implementation(libs.compose.ui)
        implementation(libs.compose.ui.graphics)
        implementation(libs.compose.ui.tooling.preview)
        implementation(libs.kermit)
      }
    }

    androidMain {
      dependencies {
        implementation(libs.androidx.activity.compose)
        implementation(libs.circuitx.android)
        implementation(libs.metro.android)
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
