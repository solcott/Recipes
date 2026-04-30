plugins {
  `kotlin-dsl`
  alias(libs.plugins.ktfmt)
}

dependencies {
  implementation(libs.android.gradle.plugin)
  implementation(libs.kotlin.gradle.plugin)
  compileOnly(libs.plugins.dependency.sorter.toDep())
  compileOnly(libs.plugins.dependency.analysis.toDep())
  compileOnly(libs.plugins.ktfmt.toDep())
  compileOnly(libs.plugins.kmp.parcelize.toDep())
}

// Should be synced with gradle/gradle-daemon-jvm.properties
kotlin { jvmToolchain(libs.versions.jvm.toolchain.get().toInt()) }

tasks.validatePlugins { enableStricterValidation = true }

ktfmt {
  googleStyle()
  removeUnusedImports = true
}

fun Provider<PluginDependency>.toDep() = map {
  "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}"
}
