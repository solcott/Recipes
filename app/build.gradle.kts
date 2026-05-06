@file:OptIn(ExperimentalMetroGradleApi::class)

import dev.zacsweers.metro.gradle.ExperimentalMetroGradleApi

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.metro)
  alias(libs.plugins.dependency.sorter)
  alias(libs.plugins.ktfmt)
  id("dependency.analysis")
  id("detekt")
}

android {
  compileSdk = libs.versions.androidCompileSdk.get().toInt()
  namespace = "com.scottolcott.recipe"
  compileSdk {
    version = release(libs.versions.androidCompileSdk.get().toInt()) { minorApiLevel = 0 }
  }

  defaultConfig {
    applicationId = "com.scottolcott.recipe"
    minSdk = libs.versions.androidMinSdk.get().toInt()
    targetSdk = libs.versions.androidCompileSdk.get().toInt()
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      optimization { enable = true }
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.toVersion(libs.versions.jvmSourceCompatibility.get())
    targetCompatibility = JavaVersion.toVersion(libs.versions.jvmTargetCompatibility.get())
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
}

kotlin {
  jvmToolchain(libs.versions.jvm.toolchain.get().toInt())
  dependencies {
    api(projects.config)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.window.core)
    implementation(libs.circuit.foundation)
    implementation(libs.circuit.runtime.presenter)
    implementation(libs.circuit.runtime.screen)
    implementation(libs.circuit.runtime.ui)
    implementation(libs.coil)
    implementation(libs.coil.core)
    implementation(libs.coil.network.core)
    implementation(libs.coil.network.ktor)
    implementation(libs.coil.network.okhttp)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.kermit)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.metro.android)
    implementation(projects.domain)
    implementation(projects.network)
    implementation(projects.repository)
    implementation(projects.shared)
    implementation(projects.storage)
    implementation(projects.ui)

    releaseImplementation(libs.kermit.core)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.kermit.android.debug)
    debugImplementation(libs.kermit.core.android.debug)

    debugRuntimeOnly(libs.compose.ui.test.manifest)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.monitor)
    androidTestImplementation(libs.junit)
  }
}

compose.resources {
  publicResClass = true
  packageOfResClass = "com.scottolcott.recipe"
  generateResClass = always
}

dependencyAnalysis {
  issues {
    onUnusedDependencies {
      exclude(libs.compose.ui.tooling)
      exclude(libs.compose.ui.tooling.preview)
    }
  }
}
