@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
  id("kmp.library")
  alias(libs.plugins.kotlinx.serialation)
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(libs.kotlin.serialization.core)
        api(libs.metro.runtime)
      }
    }
  }
}
