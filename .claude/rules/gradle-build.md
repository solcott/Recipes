---
paths:
  - "**/*.gradle.kts"
  - "build-logic/**"
  - "gradle/**"
  - "gradle.properties"
---

# Gradle build and conventions

## JDK 25, bytecode 17

Everything runs on **JDK 25** and everything compiles to **JVM 17** — those are two different
settings and the split is deliberate. The daemon uses Amazon Corretto 25, auto-provisioned via
foojay from `gradle/gradle-daemon-jvm.properties`; `jvm-toolchain = "25"` in the catalog is the
JDK that compiles the code and runs it (`:desktopApp:run`, tests, the jpackage runtime image), while
`jvmSourceCompatibility`/`jvmTargetCompatibility = "17"` is the bytecode and API level.
Don't override the JDK.

A toolchain silently sets `jvmTarget` to its own version unless a target pins one, so **every JVM
compilation pins it explicitly** — `jvm {}` and `android {}` in `kmp.library`, plus `:app` and
`:desktopApp` — each with `jvmTarget` *and* `-Xjdk-release=17`, which additionally hides post-17 JDK
APIs so nothing that would crash on Android can compile. Drop either and you silently get bytecode
25. `-Xjdk-release` is JVM-only: never add it to the project-wide `compilerOptions`, which also
feeds Native, JS, and WasmJS. `kotlin.jvm.target.validation.mode` is unset (default `error`), so a
Kotlin/Java target mismatch fails the build rather than shipping — that check is a feature, not an
obstacle.

## Module conventions

- New library modules apply the **`kmp.library`** convention plugin
  (`build-logic/src/main/kotlin/kmp.library.gradle.kts`), not raw KMP plugins. It sets the targets,
  toolchain, Android namespace (`com.scottolcott.recipe.<module>`), ktfmt, detekt, and sort-dependencies.
- **No `iosX64`** — Intel Macs can't run the iOS simulator build.
- `-Xexpect-actual-classes` is set project-wide by `kmp.library`. Don't re-declare it per file.
  The `-opt-in=kotlin.time.ExperimentalTime` that used to sit beside it is gone: `kotlin.time.Clock`
  and `Instant` went Stable in Kotlin 2.3.0, so neither the flag nor a per-file `@OptIn` is needed.
- `@Parcelize` comes from `io.github.solcott.kmp.parcelize` (a multiplatform plugin), **not**
  kotlin-parcelize. It applies only to the `:model` id value classes (`RecipeId`, `CategoryId`,
  `IngredientId`) — **`Screen`s are not Parcelable** (see `.claude/rules/circuit.md`).
- Any module declaring `Screen`s needs the kotlinx-serialization Gradle plugin
  (`libs.plugins.kotlinx.serialization`); `:domain` already applies it.

## Tooling tasks

- `detektAll` is a custom aggregate task from `build-logic/src/main/kotlin/detekt.gradle.kts`.
  Plain `detekt` does not cover all source sets.
- `buildHealth` (dependency-analysis) is noisy about Compose artifacts. Most modules already carry
  `dependencyAnalysis { issues { onUnusedDependencies { exclude(...) } } }` — add to those rather
  than deleting a dependency it flags.
