@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
  id("kmp.library")
  alias(libs.plugins.kotlinx.serialization)
}

kotlin { sourceSets { commonMain { dependencies { api(libs.kotlin.serialization.core) } } } }
