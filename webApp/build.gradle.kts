import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.metro)
  alias(libs.plugins.dependency.sorter)
  id("dependency.analysis")
}

kotlin {
  js {
    browser()
    binaries.executable()
    useEsModules()
  }

  @OptIn(ExperimentalWasmDsl::class)
  wasmJs {
    browser { commonWebpackConfig { devtool = "source-map" } }
    useEsModules()
    binaries.executable()
  }

  sourceSets {
    commonMain.dependencies {
      implementation(projects.ui)
      implementation(projects.shared)

      implementation(libs.compose.ui)
      implementation(libs.coil.core)
      implementation(libs.kermit)
    }
  }
}

dependencyAnalysis {
  issues {
    onUnusedDependencies {
      exclude("dev.zacsweers.metro:runtime")
      exclude(libs.coil.core)
      exclude(libs.compose.ui)
      exclude(libs.kermit)
      exclude(projects.ui)
      exclude(projects.shared)
    }
    onIncorrectConfiguration {}
  }
}
