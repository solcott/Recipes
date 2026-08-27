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
  js {
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
