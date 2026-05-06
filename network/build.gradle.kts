@file:OptIn(ExperimentalMetroGradleApi::class, ExperimentalKotlinGradlePluginApi::class)

import com.android.build.api.withAndroid
import dev.zacsweers.metro.gradle.ExperimentalMetroGradleApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
  id("kmp.library")
  alias(libs.plugins.kotlinx.serialation)
  alias(libs.plugins.metro)
}

kotlin {
  sourceSets {
    applyDefaultHierarchyTemplate {
      common {
        group("nonJvm") {
          @Suppress("UnstableApiUsage") withAndroid()
          withNative()
          withJs()
          withWasmJs()
        }
      }
    }
    commonMain {
      dependencies {
        api(libs.kermit)
        api(libs.kotlin.serialization.core)
        api(libs.ktor.client.core)
        api(libs.metro.runtime)
        api(projects.model)

        implementation(libs.kermit.core)
        implementation(libs.kotlin.serialization.json)
        implementation(libs.ktor.client.content.negotiation)
        implementation(libs.ktor.client.logging)
        implementation(libs.ktor.client.resources)
        implementation(libs.ktor.http)
        implementation(libs.ktor.resources)
        implementation(libs.ktor.serialization)
        implementation(libs.ktor.serialization.kotlinx.json)
        implementation(libs.ktor.utils)
        implementation(projects.config)
      }
    }
    commonJvmMain { dependencies { implementation(libs.ktor.client.okhttp) } }
    val nonJvmMain by getting { dependencies { implementation(libs.kermit.ktor) } }

    iosMain { dependencies { implementation(libs.ktor.client.darwin) } }
    jsMain { dependencies {} }
    wasmJsMain { dependencies {} }
  }
}

metro { generateContributionProviders = true }
