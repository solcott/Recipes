import com.ncorti.ktfmt.gradle.KtfmtExtension
import com.ncorti.ktfmt.gradle.KtfmtPlugin

plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.kotlin.multiplatform) apply false
  alias(libs.plugins.android.multiplatform.library) apply false
  alias(libs.plugins.compose.multiplatform) apply false
  alias(libs.plugins.kotlinx.serialation) apply false
  alias(libs.plugins.metro) apply false
  alias(libs.plugins.ktfmt) apply false
  alias(libs.plugins.dependency.sorter)
  alias(libs.plugins.kmp.parcelize) apply false
  alias(libs.plugins.dependency.analysis)
  alias(libs.plugins.kotlin.jvm) apply false
  alias(libs.plugins.detekt)
}

subprojects {
  apply<KtfmtPlugin>()
  configure<KtfmtExtension> {
    googleStyle() }
}
