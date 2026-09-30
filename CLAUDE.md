# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Kotlin Multiplatform recipe app targeting Android, iOS, Desktop (JVM), JS, and WasmJS.

Area-specific guidance lives in `.claude/rules/` and loads automatically when you touch matching
files: `gradle-build`, `hot-reload`, `data-flow`, `compose-stability`, `circuit`, `network`,
`storage`. Read the relevant one directly if you need it before opening a file in that area.

## Build & verify

Gradle 9.7.1. Everything runs on **JDK 25** and compiles to **JVM 17** — the split is deliberate;
don't override the JDK. Why, and how every JVM compilation pins it: `.claude/rules/gradle-build.md`.

- **Compile check after edits:** `./gradlew :<module>:build`. Builds every target for that module,
  which catches `expect`/`actual` mismatches and web/native breakage that a single-target compile misses.
- **Tests:** `./gradlew :domain:jvmTest` — `:domain` is the only module with tests today. Aggregate: `./gradlew allTests`.
- **Run:** `:desktopApp:run` · `:webApp:wasmJsBrowserDevelopmentRun` · `:webApp:jsBrowserDevelopmentRun` · `:app:installDebug`.
  For desktop UI work prefer `:desktopApp:hotRun` — see `.claude/rules/hot-reload.md`.

CI (`.github/workflows/build.yml`, ubuntu) runs `ktfmtCheck checkSortDependencies`, `detektAll`
and `build` on every PR and push to `main`; it needs the `MEALDB_API_KEY` repository secret. It only
reports, and Linux skips linking the iOS frameworks, so still run this loop before pushing —
`/verify`, or by hand:

```
./gradlew ktfmtFormat sortDependencies
./gradlew detektAll
./gradlew :<module>:build
```

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

## Modules and source sets

- Targets: `android`, `jvm`, `iosArm64`, `iosSimulatorArm64`, `js(browser, ESM)`, `wasmJs(browser, ESM)`.
- Custom source-set groups available: `commonJvm` (jvm+android), `web` (js+wasmJs), `nonWeb`, `nonAndroid`.
- Typesafe project accessors are enabled: write `projects.domain`, not `project(":domain")`.

## Architecture

Layering is strict. Respect it:

| Layer | Module | Contents |
|---|---|---|
| Presentation logic | `:domain` | Circuit `Presenter`, `CircuitUiState`, events, `Screen`, producers |
| Composables | `:ui` | `@Composable` screen UI only |
| Data | `:repository` | Store 5 repositories returning `Flow<Outcome<T>>` |
| Remote | `:network` | Ktor 3 with typed `ktor-client-resources`, DTOs — see `/themealdb-api` for endpoints and tiers |
| Local | `:storage` | Room 3 (`androidx.room3`) + DataStore |

A presenter and its UI live in **different modules**, joined only by
`@CircuitInject(XxxScreen::class, AppScope::class)` on each side. See `/circuit-screen` for the
full pattern when adding a screen.

**DI is Metro** (`dev.zacsweers.metro`), compile-time. Each layer exposes an `XxxProviders`
interface annotated `@ContributesTo(AppScope::class)`; implementations use
`@ContributesBinding(AppScope::class)` + `@SingleIn(AppScope::class)` + `@Inject`. Per-platform
graphs (`AndroidAppGraph`, `DesktopAppGraph`, `IOSAppGraph`, `WebAppGraph`) all implement
`shared/.../AppGraph.kt`. **There is no runtime container to register into — wire via annotations.**

## Gotchas

- `MEALDB_API_KEY` (Gradle property or env var) is **required** — the build fails without it.
  How it flows and why it is redacted from logs: `.claude/rules/network.md`.
- Recipe data and images are TheMealDB's: credit it in anything user-facing, and never infer that
  a recipe is allergen-free, vegetarian, halal, etc. — `/themealdb-api` → *Attribution*.
- `mavenLocal()` is in the repository list because `io.github.solcott:kmp-parcelize` is sometimes
  published locally. If it fails to resolve, that's why.
- `README.md`'s iOS instructions are stale: the file on disk is `iosApp/iosApp.xcodeproj`, not `.xcworkspace`.

## Git etiquette

Work on a feature branch and open a PR with `gh`. Never commit directly to `main`.
