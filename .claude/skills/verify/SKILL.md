---
name: verify
description: Run this repo's full pre-commit check — ktfmt formatting, dependency sorting, detekt, and a multiplatform build of every touched module. Use before committing, opening a PR, or whenever asked to verify that changes are sound.
---

# Verify

This repo has **no CI**, so this loop is the only thing standing between a change and a broken `main`.

Run every Gradle command through the **`recipes-gradle-runner`** agent rather than calling
`./gradlew` directly. KMP builds emit thousands of lines and the agent returns a verdict plus the
relevant failure text instead of flooding the session. It runs on a cheaper model and knows this
repo's task names, so delegating is both faster and cheaper than running the build inline.

## Steps

Run in order. **Stop at the first hard failure**, report it, and fix it before continuing —
later steps are meaningless on top of a broken build.

### 1. Determine what changed

```
git status --porcelain
```

Map changed paths to Gradle modules (top-level directory → `:module`). Ignore changes confined to
`.claude/`, `*.md`, or `.idea/` — those need no Gradle run at all, so skip straight to reporting.

### 2. Format and sort (mutating)

```
./gradlew ktfmtFormat sortDependencies
```

These rewrite files. Afterwards run `git status --porcelain` again and tell the user which files
the formatter touched — they may not expect edits beyond their own.

### 3. Detekt

```
./gradlew detektAll
```

`detektAll` is the custom aggregate task from `build-logic/src/main/kotlin/detekt.gradle.kts`;
plain `detekt` misses source sets. Findings are usually real — the config is
`buildUponDefaultConfig` with Compose rules layered on. Fix them rather than suppressing, unless
the suppression is clearly justified.

If it comes back with more than a handful of findings, hand off to the **`detekt-triage`** agent
instead of reading the raw output — it returns them grouped by rule with counts.

### 4. Build each touched module

```
./gradlew :<module>:build
```

One invocation per touched module. This covers all targets (android, jvm, iosArm64,
iosSimulatorArm64, js, wasmJs), which is the point — a change that compiles on JVM can still break
`expect`/`actual` or a web target.

### 5. Tests

```
./gradlew :domain:jvmTest
```

Only if `:domain` was touched. It's currently the only module with tests.

## Not part of this loop

`./gradlew buildHealth` — the dependency-analysis report is noisy about Compose artifacts and
produces false positives. Run it deliberately, not as part of routine verification.

## Reporting

Finish with a short status per step: passed, failed (with the failure), or skipped (with why).
Don't claim the loop passed if any step was skipped for convenience.
