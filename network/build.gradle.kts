@file:OptIn(ExperimentalMetroGradleApi::class, ExperimentalKotlinGradlePluginApi::class)

import dev.zacsweers.metro.gradle.ExperimentalMetroGradleApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
  id("kmp.library")
  alias(libs.plugins.kotlinx.serialation)
  alias(libs.plugins.metro)
}

kotlin {
  sourceSets {
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
      }
    }
    val commonJvmMain by getting { dependencies { implementation(libs.ktor.client.okhttp) } }
    androidMain { dependencies { implementation(libs.kermit.ktor) } }

    webMain { dependencies { implementation(libs.kermit.ktor) } }
    iosMain {
      dependencies {
        implementation(libs.kermit.ktor)
        implementation(libs.ktor.client.darwin)
      }
    }
    jsMain { dependencies {} }
    wasmJsMain { dependencies {} }
  }
}

metro { generateContributionProviders = true }
