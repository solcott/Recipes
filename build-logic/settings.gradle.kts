pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
    mavenLocal()
  }
}

dependencyResolutionManagement {
  @Suppress("UnstableApiUsage")
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
    // Needed for 2.0.0-alpha.4.  Can remove once 2.0.0-alpha.5 is released
    // https://github.com/detekt/detekt/issues/9396
    maven { url = uri("https://repo.gradle.org/gradle/libs-releases") }
    mavenLocal()
  }
  versionCatalogs { create("libs") { from(files("../gradle/libs.versions.toml")) } }
}

rootProject.name = "build-logic"
