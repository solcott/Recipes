import com.ncorti.ktfmt.gradle.KtfmtExtension
import com.ncorti.ktfmt.gradle.tasks.KtfmtCheckTask
import com.ncorti.ktfmt.gradle.tasks.KtfmtFormatTask
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins { id("com.ncorti.ktfmt.gradle") }

ktfmt { googleStyle() }

// ktfmt-gradle 0.27.0 formats no `androidMain` source set in these modules, so anything under
// `src/androidMain` was checked by nothing at all.
//
// Two of its behaviours combine to produce that. Its KMP handler skips every source set named
// `android*` unless `com.android.kotlin.multiplatform.library` is applied -- but it runs from
// `plugins.withId("org.jetbrains.kotlin.multiplatform")`, which fires the moment the Kotlin plugin
// is applied, one line *before* `kmp.library` applies the AGP plugin it looks for. The Android
// handler it defers to then returns early, because the KMP library plugin's extension is not the
// `CommonExtension` that handler requires.
//
// Registering the missing tasks here restores the invariant the repo relies on: `ktfmtFormat` and
// `ktfmtCheck` cover every source set. Guarded on the task not already existing, so a fixed ktfmt
// takes over cleanly.
plugins.withId("org.jetbrains.kotlin.multiplatform") {
  val kmp = extensions.getByType(KotlinMultiplatformExtension::class.java)
  kmp.sourceSets.configureEach {
    if (!name.startsWith("android")) return@configureEach

    // Captured before the register blocks, where `name` would resolve to the task's own name.
    val sourceSetName = name
    val suffix = sourceSetName.replaceFirstChar(Char::uppercaseChar)
    // A KMP source set's directories include KSP output under `build/`. Reuse ktfmt's own
    // exclusion pattern so generated Room and Metro code is neither formatted nor checked.
    val excluded = extensions.getByType(KtfmtExtension::class.java).srcSetPathExclusionPattern
    val sources = kotlin.sourceDirectories.filter { !it.absolutePath.matches(excluded.get()) }
    val formatName = "ktfmtFormatKmp$suffix"
    val checkName = "ktfmtCheckKmp$suffix"
    if (formatName in tasks.names || checkName in tasks.names) return@configureEach

    val format =
      tasks.register<KtfmtFormatTask>(formatName) {
        description = "Run Ktfmt formatter for sourceSet 'kmp $sourceSetName' on '${project.name}'"
        setSource(sources)
        setIncludes(listOf("**/*.kt", "**/*.kts"))
      }
    val check =
      tasks.register<KtfmtCheckTask>(checkName) {
        description = "Run Ktfmt validation for sourceSet 'kmp $sourceSetName' on '${project.name}'"
        setSource(sources)
        setIncludes(listOf("**/*.kt", "**/*.kts"))
      }

    tasks.named("ktfmtFormat") { dependsOn(format) }
    tasks.named("ktfmtCheck") { dependsOn(check) }
  }
}
