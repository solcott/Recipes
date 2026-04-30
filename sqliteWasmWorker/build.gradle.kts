@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  id("dependency.analysis")
}

kotlin {
  js {
    browser()
    useEsModules()
  }
  wasmJs {
    browser()
    useEsModules()
  }

  sourceSets {
    commonMain.dependencies {
      api(libs.androidx.sqlite.web)
      implementation(npm("sqlite-wasm-worker", layout.projectDirectory.dir("worker").asFile))
      implementation(npm("@sqlite.org/sqlite-wasm", "3.50.1-build1"))
      implementation(libs.kotlinx.browser)
    }
    wasmJsMain.dependencies {}
  }
}

dependencyAnalysis {
  issues {
    onUnusedDependencies {
      exclude(libs.androidx.sqlite.web.get().module.toString())
      exclude(libs.kotlinx.browser.get().module.toString())
    }
  }
}
