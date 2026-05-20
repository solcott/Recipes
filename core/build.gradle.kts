@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins { id("kmp.library") }

kotlin {
  android { androidResources { enable = true } }

  sourceSets { commonMain { dependencies { api(libs.metro.runtime) } } }
}
