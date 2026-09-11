# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Kotlin Multiplatform recipe app targeting Android, iOS, Desktop (JVM), JS, and WasmJS.

## Build & verify

Gradle 9.7.1. Everything runs on **JDK 25** and everything compiles to **JVM 17** — those are two
different settings and the split is deliberate. The daemon uses Amazon Corretto 25, auto-provisioned
via foojay from `gradle/gradle-daemon-jvm.properties`; `jvm-toolchain = "25"` in the catalog is the
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

- **Compile check after edits:** `./gradlew :<module>:build`. Builds every target for that module,
  which catches `expect`/`actual` mismatches and web/native breakage that a single-target compile misses.
- **Tests:** `./gradlew :domain:jvmTest` — `:domain` is the only module with tests today. Aggregate: `./gradlew allTests`.
- **Run:** `:desktopApp:run` · `:webApp:wasmJsBrowserDevelopmentRun` · `:webApp:jsBrowserDevelopmentRun` · `:app:installDebug`.
  For desktop UI work prefer `:desktopApp:hotRun` — see *Compose Hot Reload* below.

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

## Compose Hot Reload

Swaps changed classes into the running desktop app instead of restarting it. **Nothing in the build
declares it** — the Compose Multiplatform Gradle plugin (1.12.0) auto-applies
`org.jetbrains.compose.hot-reload` (1.2.0) to every project with the Kotlin JVM or Multiplatform
plugin, so there is no alias in `libs.versions.toml` to bump and nothing to look for in
`desktopApp/build.gradle.kts`. `-Porg.jetbrains.compose.hot.reload.disable=true` turns it off.
The `exclude("org.jetbrains.compose.hot-reload:hot-reload-runtime-api")` lines in `:desktopApp`,
`:ui`, and `:domain` are dependency-analysis reacting to the artifact the plugin injects.

`gradle.properties` sets `compose.reload.jbr.autoProvisioningEnabled=true`: hot reload needs a
JetBrains Runtime for enhanced class redefinition, and no JBR is installed system-wide, so Gradle
fetches one through the foojay resolver on first use (~200MB, once). It follows the project
toolchain, so it now provisions **JBR 25** (`jbrsdk_jcef-25.0.4.1`) and the daemon, the compile
toolchain, and the hot-reload JVM are all 25. An older `jbrsdk_jcef-21` may still be sitting in
`~/.gradle/jdks` from before the toolchain moved; it is unused.

```
./gradlew :desktopApp:hotRun --auto   # continuous build; reloads on save
./gradlew :desktopApp:hotRun          # explicit mode; reload with ./gradlew :desktopApp:reload
```

`:desktopApp` is a plain `kotlin("jvm")` module, so its tasks have **no target suffix** —
`hotRun`, `hotRunAsync`, `hotMcpServer`, `reload`. The KMP modules that apply Compose (`:ui`,
`:domain`, `:shared`) get their own `hotRunJvm`/`hotMcpServerJvm`; you never want those, and it is
why `.mcp.json` names `:desktopApp:hotMcpServer` in full rather than the bare `hotMcpServer` the
upstream README suggests — the short form matches every one of those projects at once.

Editing a composable in `:ui` reloads into the `:desktopApp` window; cross-module reload works
because both are in the same build.

**Reload output does not appear in the `hotRun` terminal.** `--auto` forks a second Gradle daemon for
the continuous build, and its recompile/reload log goes to `desktopApp/build/run/main/main.chr.log` —
read that to see "Change detected", the build result, and which `@Composable` scopes were invalidated.
The staged classes land in `desktopApp/build/run/main/classpath/hot/`. The `hotRun` terminal only
carries the app's own stdout. If a reload seems not to land, check that log before assuming the app
is wedged, and check for a stale app process from an earlier session — each `hotRun` starts its own.

**MCP server.** `.mcp.json` registers `compose-hot-reload`, which exposes `status`, `reload`,
`await_reload`, `take_screenshot`, `get_semantic_tree`, `get_ui_error`, `get_logs`, `list_windows`,
`click`, `type_text`, `scroll`, `scroll_to_index`, `resize_window`, `restart`, and `reset_ui`.
The agent starts the server itself; it waits for the app and connects when it appears, so launch
`hotRun` separately. Check `status.buildContinuous` to pick between `reload` (explicit mode) and
`await_reload` (started with `--auto`).

Two limits worth knowing here:

- **The Metro graph is not reloaded.** `createGraph<DesktopAppGraph>()` runs outside the composition
  in `main.kt`, so DI and provider changes need `restart`, not `reload`.
- **Retained state survives a reload.** `retain { }` and presenter state persist across a swap —
  usually what you want, but a state-shape change can leave stale values behind. `reset_ui` discards
  the composition; `restart` restarts the process.

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
- `-Xexpect-actual-classes` is set project-wide by `kmp.library`. Don't re-declare it per file.
  The `-opt-in=kotlin.time.ExperimentalTime` that used to sit beside it is gone: `kotlin.time.Clock`
  and `Instant` went Stable in Kotlin 2.3.0, so neither the flag nor a per-file `@OptIn` is needed.

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
| Data | `:repository` | Store 5 repositories returning `Flow<Outcome<T>>` |
| Remote | `:network` | Ktor 3 with typed `ktor-client-resources`, DTOs — see `/themealdb-api` for endpoints, tiers, and DTO traps |
| Local | `:storage` | Room 3 (`androidx.room3`) + DataStore |

A presenter and its UI live in **different modules**, joined only by
`@CircuitInject(XxxScreen::class, AppScope::class)` on each side. See `/circuit-screen` for the
full pattern when adding a screen.

### Reading data: `Outcome` and `ContentState`

Repositories return `Flow<Outcome<T>>`, not Store's own `StoreReadResponse`. **Store 5 is an
implementation detail of `:repository`** — it is an `implementation` dependency there, and nothing
above that module compiles against it. `io.github.solcott:dataresult-store5` does the translation
in `asOutcomes()`, at the end of each repository's chain.

The types come from [kmp-dataresult](https://github.com/solcott/kmp-dataresult), shared with the
`Countries` project. Resolving them needs `gpr.user`/`gpr.key` in `~/.gradle/gradle.properties` (a
classic PAT with `read:packages`) — GitHub Packages authenticates even public reads.

The path from a repository to a screen state is three steps:

1. `Flow<Outcome<T>>` — `Loading`, `Data(value, origin)` or `Error(cause, origin)`. Store's
   `Initial` and `Loading` both become `Outcome.Loading`; `NoNewData` is dropped.
2. `produceRetainedContentState(initial, keys) { … }` from `libs.uistateCircuit`
   (`io.github.solcott.uistate.circuit`) folds those emissions into a retained `ContentState<T>`.
   **`ContentState.data` is what holds the last loaded value** — the separate retained
   `lastItems`/`lastRecipes` caches that used to sit beside the response are gone, and so is
   `ListUi`. Shared with `Countries`; the local copy in `:domain`'s `producer` package is gone.
3. `ContentState.foldToState(onLoading, onError, onContent)` in `presenter/ListContent.kt` maps it
   onto a screen's own three-case `CircuitUiState`.

A screen fed by several sources at once uses the group versions of the first two steps. The
repository returns `combineOutcomes(a, b, c)`, a `Flow<Outcomes3<A, B, C>>` with each source seeded
with `Loading` so the fastest one shows at once. The presenter folds it with
`produceRetainedContentStates(contentStatesOf(…))` into one `ContentState` per source. Destructure
the group for names; `isAnyLoading`/`errorOrNull` on it answer for all of them. `SearchPresenter` is
the example.

Two things are easy to get wrong:

- **Check `hasLoaded`, never `data.isEmpty()`**, to tell "nothing has loaded yet" from "loaded and
  empty". `hasLoaded` reads `origin`, which stays null until the first value arrives. Testing the
  data for emptiness leaves a spinner over a legitimately empty tab forever.
- **Having data beats being in flight.** `foldToState` checks `hasLoaded` first, which is what
  renders a background refresh as `isRefreshing` over the existing grid instead of dropping back to
  a full-screen spinner. `domain`'s `refreshKeepsPreviousAreas` test pins this — don't reorder those
  branches.

`DataError` is structured; screen states carry `String`. `DataError.toMessage()` in
`ListContent.kt` bridges the two, and belongs in `:ui` with the rest of the user-facing copy once
these get localized.

**A test fake for an in-flight request must not complete.** `produceRetainedContentState` settles a still
loading status when its source completes, so a spinner can never hang — which means
`flowOf(Outcome.Loading)` is a *settled empty result*, not a pending one. Store streams never
complete; use the `inFlight()` helper in the presenter tests, which emits `Loading` and then awaits
cancellation.

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
