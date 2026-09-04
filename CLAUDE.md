# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Kotlin Multiplatform recipe app targeting Android, iOS, Desktop (JVM), JS, and WasmJS.

## Build & verify

Gradle 9.7.1. The daemon runs on **Amazon Corretto 25**, auto-provisioned via foojay from
`gradle/gradle-daemon-jvm.properties`; Kotlin and Java compile to **JVM 17** (`jvmToolchain(17)`).
Don't override the JDK.

- **Compile check after edits:** `./gradlew :<module>:build`. Builds every target for that module,
  which catches `expect`/`actual` mismatches and web/native breakage that a single-target compile misses.
- **Tests:** `./gradlew :domain:jvmTest` — `:domain` is the only module with tests today. Aggregate: `./gradlew allTests`.
- **Run:** `:desktopApp:run` · `:webApp:wasmJsBrowserDevelopmentRun` · `:webApp:jsBrowserDevelopmentRun` · `:app:installDebug`.

There is **no CI**. Nothing catches formatting, detekt, or compile regressions except this loop —
run `/verify`, or by hand:

```
./gradlew ktfmtFormat sortDependencies
./gradlew detektAll
./gradlew :<module>:build
```

- `detektAll` is a custom aggregate task from `build-logic/src/main/kotlin/detekt.gradle.kts`.
  Plain `detekt` does not cover all source sets.
- `buildHealth` (dependency-analysis) is noisy about Compose artifacts. Most modules already carry
  `dependencyAnalysis { issues { onUnusedDependencies { exclude(...) } } }` — add to those rather
  than deleting a dependency it flags.

## Delegation

Project agents in `.claude/agents/` run on cheaper models. Prefer them over doing the work inline
or reaching for a general-purpose agent:

| Task | Agent |
|---|---|
| Any `./gradlew` invocation | `recipes-gradle-runner` |
| "Where is X" / "which module owns Y" | `recipes-locator` |
| Reading detekt output | `detekt-triage` |
| Boilerplate for a brand-new Circuit screen | `circuit-scaffold` |

A KMP build log is thousands of lines across six targets — never run one inline just to see whether
something compiles. Keep judgment calls (what a failure means, whether a finding is real) in the
main session; delegate the execution and the summarizing.

## Code style

- **ktfmt Google style**: 2-space indent, 100 columns, unused imports stripped. Applied to all
  subprojects by the `formatting` convention plugin. Never hand-format — run `ktfmtFormat`.
- Detekt config lives at `config/detekt/detekt.yml` (built on defaults) plus `io.nlopez.compose.rules`.
  The IDE plugin treats findings as errors.
- `-Xexpect-actual-classes` and `-opt-in=kotlin.time.ExperimentalTime` are set project-wide by
  `kmp.library`. Don't re-declare them per file.

## Module & build conventions

- New library modules apply the **`kmp.library`** convention plugin
  (`build-logic/src/main/kotlin/kmp.library.gradle.kts`), not raw KMP plugins. It sets the targets,
  toolchain, Android namespace (`com.scottolcott.recipe.<module>`), ktfmt, detekt, and sort-dependencies.
- Targets: `android`, `jvm`, `iosArm64`, `iosSimulatorArm64`, `js(browser, ESM)`, `wasmJs(browser, ESM)`.
  **No `iosX64`** — Intel Macs can't run the iOS simulator build.
- Custom source-set groups available: `commonJvm` (jvm+android), `web` (js+wasmJs), `nonWeb`, `nonAndroid`.
- Typesafe project accessors are enabled: write `projects.domain`, not `project(":domain")`.
- `@Parcelize` comes from `io.github.solcott.kmp.parcelize` (a multiplatform plugin), **not**
  kotlin-parcelize. It applies only to the `:model` id value classes (`RecipeId`, `CategoryId`,
  `IngredientId`) — **`Screen`s are not Parcelable**, see *Navigation persistence* below.
- Any module declaring `Screen`s needs the kotlinx-serialization Gradle plugin
  (`libs.plugins.kotlinx.serialization`); `:domain` already applies it.

## Architecture

Layering is strict. Respect it:

| Layer | Module | Contents |
|---|---|---|
| Presentation logic | `:domain` | Circuit `Presenter`, `CircuitUiState`, events, `Screen`, producers |
| Composables | `:ui` | `@Composable` screen UI only |
| Data | `:repository` | Store 5 repositories returning `Flow<StoreReadResponse<T>>` |
| Remote | `:network` | Ktor 3 with typed `ktor-client-resources`, DTOs — see `/themealdb-api` for endpoints, tiers, and DTO traps |
| Local | `:storage` | Room 3 (`androidx.room3`) + DataStore |

A presenter and its UI live in **different modules**, joined only by
`@CircuitInject(XxxScreen::class, AppScope::class)` on each side. See `/circuit-screen` for the
full pattern when adding a screen.

**Circuit screen file layout** (`domain/.../presenter/XxxPresenter.kt`), in this order:
presenter class → sealed `XxxState : CircuitUiState` (Loading/Error/Success) → sealed `XxxEvent`
with nested per-state sub-interfaces → the `Screen` last, as
`@CircuitSerializable(AppScope::class) data object XxxScreen : Screen`.
`eventSink` properties are annotated `@Redacted` so they stay out of `toString`.

**DI is Metro** (`dev.zacsweers.metro`), compile-time. Each layer exposes an `XxxProviders`
interface annotated `@ContributesTo(AppScope::class)`; implementations use
`@ContributesBinding(AppScope::class)` + `@SingleIn(AppScope::class)` + `@Inject`. Per-platform
graphs (`AndroidAppGraph`, `DesktopAppGraph`, `IOSAppGraph`, `WebAppGraph`) all implement
`shared/.../AppGraph.kt`. **There is no runtime container to register into — wire via annotations.**

- State survives recomposition/config change via `androidx.compose.runtime.retain.retain { }`,
  **not** `rememberSaveable`. One sanctioned exception: `HomePresenter` persists the selected tab
  with `rememberSerializable`, because that selection is cheap to store and worth surviving process
  death so the app reopens on the tab the user left. Don't "fix" it to `retain { }`.
- A new screen must also be added to `domain/.../navigation/ScreenUrlMapper.kt` — both the
  `Screen.toUrlPath()` and `urlPathToScreen()` directions. It backs `recipes://app/...` deep links
  and browser history on web. Home tabs are the exception to writing anything there by hand: each
  `HomeTabScreen` carries its own `urlSegment` and the mapper reads `HOME_TABS`, so adding a tab to
  that one list makes it addressable in both directions.
- **`:domain` depends on Material3 (`api`), deliberately.** `SearchState` holds a `SearchBarState`
  and `RecipeScaffoldState`/`SearchScreen` carry a `SearchBarValue`, so that the presenter — not the
  composable — owns whether the search bar is expanded. This is the *only* sanctioned place for
  Material3 widget state in `:domain`; other widget state belongs in `:ui`.
- Two `Json` qualifiers exist, `@NetworkJson` and `@StorageJson`
  (`core/.../serialization/JsonQualifiers.kt`). Pick deliberately.

**Navigation persistence** (Circuit 0.38+): `Screen` and `PopResult` are no longer `Parcelable`, so
the back stack is persisted with kotlinx-serialization. See `/circuit-screen` for the
`@CircuitSerializable` rules and the saver wiring.

## Gotchas

- `MEALDB_API_KEY` (Gradle property or env var) is **required** — the build fails without it.
  It's read at build time by `build-logic/.../ProjectExt.kt` and baked into `SharedBuildConfig` via
  BuildKonfig. The app talks to TheMealDB **v2 only**; there is no v1 fallback, because v2 on the
  free dev key silently caps `filter.php` at one result. The key is interpolated into the URL
  *path*, so `NetworkProviders.redacting()` keeps it out of Ktor's logs — don't remove that wrapper.
  Details and the full endpoint reference: `/themealdb-api`.
- **Room has no destructive-migration fallback, deliberately.** Changing an entity without bumping
  `AppDatabase.version` throws "Room cannot verify the data integrity" on first db access; bumping
  the version without writing a `Migration` throws "A migration from N to M was required but not
  found." Both are intentional — the crash is the reminder to write the migration. Don't "fix" one
  by adding `fallbackToDestructiveMigration`; note that it would not even cover the first case,
  since `checkIdentity` runs in `onOpen`, before any migration path.
  The db is `recipe.db` on every platform (one `DATABASE_NAME` constant in `StorageFactory.kt`).
  Per-platform delete commands: `README.md` → *Resetting local data*.
- **The nav stack is `rememberSaveableNavStack`, not `rememberSaveableBackStack`.** `BackStack`
  stubs `forward()` and `backward()` to `false`, and `NavigatorImpl` delegates straight to them, so
  switching to it silently turns the browser back/forward buttons on web into no-ops —
  `ui/src/webMain/.../BrowserHistoryEffect.web.kt` drives navigation through
  `Navigator.backward()`/`forward()`. Both `RecipeApp.kt` and `RecipeScaffoldPresenter.kt` must stay
  on `rememberSaveableNavStack`.
- The circuit saver is installed with the static `setCircuitSaver(saver)` overload rather than the
  `setCircuitSaver { fallback -> ... }` transform, so an unregistered screen **fails loudly at save
  time** instead of quietly falling through to the registry-backed saver. That is deliberate.
  Restoring an unregistered screen can only return null and drop the record, so `CircuitProviders`
  logs it through `onRestoreError`.
- `mavenLocal()` is in the repository list because `io.github.solcott:kmp-parcelize` is sometimes
  published locally. If it fails to resolve, that's why.
- `README.md`'s iOS instructions are stale: the file on disk is `iosApp/iosApp.xcodeproj`, not `.xcworkspace`.

## Git etiquette

Work on a feature branch and open a PR with `gh`. Never commit directly to `main`.
