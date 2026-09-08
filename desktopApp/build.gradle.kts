import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.metro)
  alias(libs.plugins.dependency.sorter)
  id("dependency.analysis")
  id("detekt")
}

// `kotlin("jvm")` brings the Java plugin, so targetCompatibility would otherwise default to the
// toolchain's 25 and disagree with the Kotlin target below. Nothing here is written in Java; this
// exists to keep the Kotlin/Java target consistency check satisfied.
java {
  sourceCompatibility = JavaVersion.toVersion(libs.versions.jvmSourceCompatibility.get())
  targetCompatibility = JavaVersion.toVersion(libs.versions.jvmTargetCompatibility.get())
}

kotlin {
  // Compiles and runs on JDK 25 - `run` and `hotRun` set no javaLauncher, so they inherit this
  // toolchain, which is the whole point. The emitted bytecode stays at 17, read from the catalog
  // rather than hardcoded so it cannot drift from the toolchain line above it.
  jvmToolchain(libs.versions.jvm.toolchain.get().toInt())
  compilerOptions {
    jvmTarget = JvmTarget.fromTarget(libs.versions.jvmTargetCompatibility.get())
    freeCompilerArgs.add("-Xjdk-release=${libs.versions.jvmTargetCompatibility.get()}")
  }
  dependencies {
    api(projects.config)
    api(projects.core)
    api(projects.domain)
    api(projects.network)
    api(projects.repository)
    api(projects.shared)
    api(projects.storage)
    api(projects.ui)
    api(libs.androidx.compose.runtime.desktop)
    api(libs.circuit.foundation)
    api(libs.circuit.runtime.presenter)
    api(libs.circuit.runtime.screen)
    api(libs.circuit.runtime.ui)
    api(libs.coil.core)
    api(libs.compose.ui.desktop)
    api(libs.ktor.client.core)

    implementation(compose.desktop.currentOs)
    implementation(libs.androidx.window.core)
    implementation(libs.compose.ui.graphics.desktop)
    implementation(libs.compose.ui.unit.desktop)
    implementation(libs.kermit)
    implementation(libs.kermit.core)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.ktor.client.okhttp)
  }
}

compose.desktop {
  application {
    mainClass = "com.scottolcott.recipe.MainKt"
    buildTypes.release.proguard {
      version = "7.9.1"
      obfuscate.set(true)
      configurationFiles.from(project.file("compose-desktop.pro"))
    }
    nativeDistributions {
      targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
      packageName = "com.scottolcott.recipe.composedemo"
      packageVersion = version.toString()
      modules("java.base", "java.desktop", "java.sql", "java.xml", "java.naming")
    }
  }
}

dependencyAnalysis {
  issues {
    onUnusedDependencies {
      exclude("org.jetbrains.compose.desktop:desktop-jvm-macos-arm64")
      exclude("org.jetbrains.compose.hot-reload:hot-reload-runtime-api")
    }
    onRuntimeOnly { exclude(libs.kotlinx.coroutines.swing) }
  }
}

// Compose Hot Reload launches through its own tasks (hotRun, hotRunAsync), not `run`, so the guard
// has to cover both or RuntimeConfigImpl.debugBuild silently flips to false under hot reload.
tasks.withType<JavaExec>().configureEach {
  if (name == "run" || name.startsWith("hotRun")) {
    systemProperty("debug", "true")
    // `-Pdesign=cupertino` renders the iOS design here on desktop. That is the whole point of
    // `AppDesign` being a composition local rather than a call to `isIos()`: the Cupertino chrome
    // can be iterated in a hot-reload loop instead of an Xcode round-trip.
    systemProperty("recipes.design", providers.gradleProperty("design").getOrElse("material"))
    // `-Pinput=touch` does the same for the other axis: desktop runs in the pointer design by
    // default, and this is how it is compared against the touch metrics the phones get.
    systemProperty("recipes.input", providers.gradleProperty("input").getOrElse("pointer"))
  }
}
