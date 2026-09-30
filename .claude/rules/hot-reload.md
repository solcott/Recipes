---
paths:
  - "desktopApp/**"
  - "ui/**"
---

# Compose Hot Reload

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
