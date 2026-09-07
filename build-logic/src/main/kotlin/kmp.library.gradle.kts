@file:Suppress("OPT_IN_USAGE")

import com.android.build.api.withAndroid
import com.scottolcott.gradle.versionCatalog
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("com.android.kotlin.multiplatform.library")
  id("io.github.solcott.kmp.parcelize")
  id("com.squareup.sort-dependencies")
  id("com.ncorti.ktfmt.gradle")
  id("dependency.analysis")
  id("detekt")
  id("formatting")
}

kotlin {
  jvmToolchain(project.versionCatalog.findVersion("jvm-toolchain").get().requiredVersion.toInt())
  compilerOptions { freeCompilerArgs.add("-Xexpect-actual-classes") }
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
  js {
    // Stays on Karma despite Kotlin 2.4.20's Playwright/Mocha replacement: the testBalloon
    // plugin hands its parameters to browser tests by generating karma.config.d/
    // testBalloonParameters.js, and the new DSL does not read karma.config.d. Under it
    // :domain:jsBrowserTest never receives TESTBALLOON_* and hangs to the 30s timeout.
    // Revisit when de.infix.testBalloon supports the new DSL.
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

// Compose UI tests on js are only loadable when the target produces a webpack bundle, which is what
// supplies the Skiko runtime. Gated on the Compose plugin so non-Compose modules don't build an
// executable they have no use for. https://youtrack.jetbrains.com/issue/CMP-4906
plugins.withId("org.jetbrains.compose") {
  kotlin {
    js { binaries.executable() }
    wasmJs { binaries.executable() }
  }
}
