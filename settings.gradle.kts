import java.net.URI

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
//    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven {
            url = URI("https://central.sonatype.com/repository/maven-snapshots/")
        }
    }
}

rootProject.name = "Recipes"
include(":app", ":network", ":storage", ":repository", ":domain", ":model", ":ui", ":shared", ":sqliteWasmWorker", ":webApp", ":desktopApp", ":config")
