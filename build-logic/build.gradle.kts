import dev.detekt.gradle.Detekt
import org.gradle.kotlin.dsl.withType

plugins {
  `kotlin-dsl`
  alias(libs.plugins.ktfmt)
  alias(libs.plugins.detekt)
}

dependencies {
  implementation(libs.android.gradle.plugin)
  implementation(libs.kotlin.gradle.plugin)
  compileOnly(libs.plugins.dependency.sorter.toDep())
  compileOnly(libs.plugins.dependency.analysis.toDep())
  compileOnly(libs.plugins.ktfmt.toDep())
  compileOnly(libs.plugins.kmp.parcelize.toDep())
  compileOnly(libs.plugins.detekt.toDep())
}

kotlin { jvmToolchain(libs.versions.jvm.toolchain.get().toInt()) }

tasks.validatePlugins { enableStricterValidation = true }

detekt {
  config.setFrom(files("$rootDir/../config/detekt/detekt.yml"))
  buildUponDefaultConfig = true
  allRules = false
}

tasks.withType<Detekt> {
  reports { html.required = true }
  exclude("**/build/**")
  exclude("**/generated/**")
}

ktfmt {
  googleStyle()
  removeUnusedImports = true
}

fun Provider<PluginDependency>.toDep() = map {
  "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}"
}
