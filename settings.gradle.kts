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
//  includeBuild("../kmp-dataresult")
@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
//    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()

        // Sonatype Central's snapshot repository, for trying a -SNAPSHOT of a library (e.g.
        // kmp-dataresult) before it is released. snapshotsOnly() keeps release lookups away.
        maven("https://central.sonatype.com/repository/maven-snapshots/") {
            name = "MavenCentralSnapshots"
            mavenContent { snapshotsOnly() }
        }
    }
}

rootProject.name = "Recipes"
include(":app", ":network", ":storage", ":repository", ":domain", ":model", ":ui", ":shared", ":sqliteWasmWorker", ":webApp", ":desktopApp", ":config", ":core")
