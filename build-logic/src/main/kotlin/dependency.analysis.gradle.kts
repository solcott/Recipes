import com.autonomousapps.DependencyAnalysisSubExtension

plugins {
    id("com.autonomousapps.dependency-analysis")
}

configure<DependencyAnalysisSubExtension> {
    issues {
        onUnusedDependencies {
            exclude("org.jetbrains.kotlin:kotlin-dom-api-compat")
            exclude("io.github.solcott:kmp-parcelize-runtime")
        }
        onIncorrectConfiguration {
            exclude("dev.zacsweers.metro:runtime")
        }
    }
}