import com.scottolcott.gradle.versionCatalog
import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension

plugins { id("dev.detekt") }

configure<DetektExtension> {
  config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
  buildUponDefaultConfig = true
  allRules = false
}

tasks.withType<Detekt> {
  reports {
    html.required = true
  }

  exclude {
    // excludes build directories
    it.file.relativeTo(projectDir).startsWith(project.layout.buildDirectory.get().asFile.relativeTo(projectDir))
  }
}
dependencies {
  val composeRulesDep = versionCatalog.findLibrary("detekt.compose.rules").get()
  add("detektPlugins", composeRulesDep)
}

tasks.register("detektAll") {
  group = "verification"
  dependsOn(tasks.withType<Detekt>())
}