---
paths:
  - "ui/**"
  - "domain/**"
  - "model/**"
  - "repository/**"
  - "shared/**"
  - "app/**"
  - "desktopApp/**"
  - "webApp/**"
  - "config/compose/**"
---

# Compose stability

Everything a presenter hands to a composable has to be *stable*, or Compose re-runs the UI on every
recomposition. Two rules keep it that way:

- **Collections headed for the UI are `ImmutableList`**, from the repository's public signature
  onward — `SourceOfTruth.reader`, `toModel()`, the `Flow<Outcome<…>>` return type, the producer's
  `ContentState<ImmutableList<T>>`, and the screen state's field. Entities and DTOs stay `List`:
  Store's local type is `List<Entity>` and nothing below `toModel()` ever reaches composition, so
  converting there is a copy that buys nothing. `:model` carries
  `api(libs.kotlinx.collections.immutable)` for this.
- **`@Immutable` when it is true, `@Stable` when it is not.** `@Immutable` promises no public
  property ever changes: right for the `:model` classes and for a screen state whose fields are all
  values (`AreasState`, `RecipesState`, `RecipeDetailsState`, `SearchTabState`, `RecipesScreen`).
  A state that carries an observable holder — `SearchBarState` and `TextFieldState` in `SearchState`,
  `NavStack`/`Navigator` in `RecipeScaffoldState` and `HomeState`, the Metro graph in
  `DesktopAppGraph` — gets `@Stable`. Both make the type skippable; only one of them is honest, and
  a class marked `@Stable` must back its mutable properties with snapshot state (this is why
  `BackShortcutHost.onBack` is a `MutableState`).

Annotate the *declared parameter type*: `@Stable` on a supertype does not propagate, so
`DesktopAppGraph` carries its own annotation rather than inheriting one from `AppGraph`.

A sealed `CircuitUiState` interface is unstable until annotated — the compiler cannot see the
implementations — so every one of them carries a marker.

## `composeStabilityCheck`

**None of the above is taken on trust — `./gradlew composeStabilityCheck` enforces it.** The
`compose.stability` convention plugin turns on the Compose compiler's own metrics and reports for
the six modules that apply the compiler plugin (`:ui`, `:domain`, `:shared`, `:app`, `:webApp`,
`:desktopApp`) and registers a per-project check that reads them. It fails on a class the compiler
inferred as `unstable`, and on a `restartable` composable that is not `skippable`. A `runtime`
class passes: its stability depends on a generic argument and is settled at run time. There is no
aggregate task — Gradle's cross-project name matching fans `composeStabilityCheck` out to all six.

Exceptions live in `config/compose/unstable-classes.txt` and
`config/compose/unskippable-composables.txt`, one `<module>:<fully.qualified.Name>` per line, each
with a comment saying why. Today only the Metro graph impls and the two Android entry points are
listed, none of which is ever a composable parameter.

Reports land in `<module>/build/reports/compose/reports/` (`-classes.txt`, `-composables.txt`) with
`-module.json` counts under `.../metrics/<target>/main/`. Read them for *why* something failed —
the entry lists each property or parameter and its verdict. The compiler names them after the
Kotlin module, which is the Gradle project path — so they carry a colon, `Recipes:domain-classes.txt`,
and CI stages a renamed copy before uploading them because `upload-artifact` rejects one.

Two traps in those reports:

- **Only the metrics path is per-target; the reports path is not.** The Compose plugin appends
  `<target>/<compilation>` to `metricsDestination` but hands `reportsDestination` to every
  compilation verbatim, and a KMP module's targets share a Kotlin module name. So all six targets
  write the same `Recipes_ui-classes.txt` and the last to run wins. After a full `./gradlew build`
  you are reading whichever target finished last. `composeStabilityCheck` is the invocation to
  trust: it depends on exactly one compile task (JVM where there is one, `compileDebugKotlin` for
  `:app`, `compileKotlinJs` for `:webApp`), so it has a single writer. That is also why it is not
  wired into `check`, and why CI runs it as its own step *before* `build`.
- **`reportsDestination` is a compiler *input*, not an output.** A compile task whose reports have
  been deleted still counts as up to date and quietly writes nothing. `compose.stability` declares
  the directory as an output of that one designated compile task to fix it, which also means the
  task re-runs after a full `build` and stays the last writer.

Detekt's `UnstableCollections` covers the same ground at the source level and is on. It is right
about composables that emit UI and wrong about ones that return a value — those are neither
restartable nor skippable, so a parameter's stability is inert. `RecipesProducer.produceByIngredients`
is the single `@Suppress` for that reason.
