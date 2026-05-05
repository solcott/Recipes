import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.metro)
  alias(libs.plugins.dependency.sorter)
  id("dependency.analysis")
  id("detekt")
}

kotlin {
  dependencies {
    api(libs.androidx.compose.runtime.desktop)
    api(libs.circuit.foundation)
    api(libs.circuit.runtime.presenter)
    api(libs.circuit.runtime.screen)
    api(libs.circuit.runtime.ui)
    api(libs.coil.core)
    api(libs.compose.ui.desktop)
    api(libs.ktor.client.core)
    api(projects.config)
    api(projects.domain)
    api(projects.network)
    api(projects.repository)
    api(projects.shared)
    api(projects.storage)
    api(projects.ui)

    implementation(compose.desktop.currentOs)
    implementation(libs.androidx.window.core)
    implementation(libs.compose.ui.graphics.desktop)
    implementation(libs.compose.ui.unit.desktop)
    implementation(libs.kermit)
    implementation(libs.kermit.core)
    implementation(libs.kotlinx.coroutines.swing)
  }
}

compose.desktop {
  application {
    mainClass = "com.scottolcott.recipe.MainKt"
    nativeDistributions {
      targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
      packageName = "com.scottolcott.recipe.composedemo"
      packageVersion = version.toString()
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
