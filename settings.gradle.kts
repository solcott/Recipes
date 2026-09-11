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

        // io.github.solcott:dataresult / :uistate / :dataresult-store5.
        // GitHub Packages authenticates even public reads, so this needs a classic PAT with the
        // read:packages scope in ~/.gradle/gradle.properties. mavenLocal() above wins while
        // iterating on the library. See github.com/solcott/kmp-dataresult.
        maven("https://maven.pkg.github.com/solcott/kmp-dataresult") {
            name = "GitHubPackages"
            credentials {
                username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
                password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
            }
            // Nothing else may resolve here, so a credential problem can't cascade.
            content { includeGroup("io.github.solcott") }
        }
//        maven {
//            url = URI("https://central.sonatype.com/repository/maven-snapshots/")
//            mavenContent { snapshotsOnly() }
//            content { includeGroup("com.slack.circuit") }
//        }
    }
}

rootProject.name = "Recipes"
include(":app", ":network", ":storage", ":repository", ":domain", ":model", ":ui", ":shared", ":sqliteWasmWorker", ":webApp", ":desktopApp", ":config", ":core")
