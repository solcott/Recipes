@file:Suppress("OPT_IN_USAGE")

import com.android.build.api.withAndroid
import com.scottolcott.gradle.versionCatalog
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType

plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("com.android.kotlin.multiplatform.library")
  id("io.github.solcott.kmp.parcelize")
  id("com.squareup.sort-dependencies")
  id("com.ncorti.ktfmt.gradle")
  id("dependency.analysis")
  id("detekt")
}

kotlin {
  jvmToolchain(project.versionCatalog.findVersion("jvm-toolchain").get().requiredVersion.toInt())
  compilerOptions {
    freeCompilerArgs.addAll("-Xexpect-actual-classes", "-opt-in=kotlin.time.ExperimentalTime")
  }
  android {
    val libs = project.versionCatalog
    namespace = "com.scottolcott.recipe.${project.name.replace("-", ".")}"
    minSdk = libs.findVersion("androidMinSdk").get().requiredVersion.toInt()
    compileSdk = libs.findVersion("androidCompileSdk").get().requiredVersion.toInt()
    compilerOptions {
      jvmTarget =
        JvmTarget.fromTarget(libs.findVersion("jvmTargetCompatibility").get().requiredVersion)
    }
  }
  jvm()
  iosArm64()
  iosSimulatorArm64()
  js(compiler = KotlinJsCompilerType.IR) {
    browser()
    useEsModules()
  }

  wasmJs {
    browser()
    useEsModules()
  }

  sourceSets {
    applyDefaultHierarchyTemplate {
      common {
        group("commonJvm") {
          withJvm()
          @Suppress("UnstableApiUsage") withAndroid()
        }
        group("web") {
          withJs()
          withWasmJs()
        }
        group("nonWeb") {
          @Suppress("UnstableApiUsage") withAndroid()
          withNative()
          withJvm()
        }
        group("nonAndroid") {
          withNative()
          withJvm()
          withJs()
          withWasmJs()
        }
      }
    }
  }
}
