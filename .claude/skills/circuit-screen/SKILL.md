---
name: circuit-screen
description: The end-to-end pattern for adding or modifying a Circuit screen in this repo — presenter, state, events, Screen, producer, composable, URL mapping, and test. Use whenever adding a new screen, adding an event to an existing one, or wiring a presenter to its UI.
---

# Adding a Circuit screen

Presenters live in `:domain`, composables live in `:ui`, and nothing but the
`@CircuitInject` annotation connects them. There is no registry to update — Metro generates the
wiring at compile time from the annotations.

Copy the shape from the canonical example rather than inventing one:
`domain/src/commonMain/kotlin/com/scottolcott/recipe/domain/presenter/CategoriesPresenter.kt`.

For a brand-new screen, the **`circuit-scaffold`** agent generates steps 1–4 mechanically on a
cheaper model, leaving TODOs where real logic belongs. This document stays the reference for
reviewing and finishing what it produces — and for changes to screens that already exist.

## 1. Presenter file — `domain/.../presenter/XxxPresenter.kt`

One file, four declarations, **in this order**:

```kotlin
@CircuitInject(XxxScreen::class, AppScope::class)
@Inject
class XxxPresenter
internal constructor(private val navigator: Navigator, private val xxxProducer: XxxProducer) :
  Presenter<XxxState> {
  @Composable override fun present(): XxxState { /* ... */ }
}

sealed interface XxxState : CircuitUiState {
  data object Loading : XxxState
  data class Error(val message: String, @Redacted val eventSink: (XxxEvent.Error) -> Unit) : XxxState
  data class Success(/* ... */, @Redacted val eventSink: (XxxEvent.Success) -> Unit) : XxxState
}

sealed interface XxxEvent {
  sealed interface Success : XxxEvent { /* clicks etc. */ }
  sealed interface Error : XxxEvent { data object RetryClicked : Error }
}

@CircuitSerializable(AppScope::class) data object XxxScreen : Screen
```

The rules this shape must obey — `@Redacted` sinks, per-state events, `retain { }`,
`@CircuitSerializable` and what it implies, parameterised screens, serializable parameters — live
in `.claude/rules/circuit.md`, which loads whenever you touch `:domain` or `:ui`. Read it before
reviewing a new presenter.

### Turning repository data into screen state

Repositories return `Flow<Outcome<T>>` — Store 5 stops at `:repository`. A producer folds those
emissions into a `ContentState<T>`, and the presenter maps that onto its own state with
`foldToState`:

```kotlin
return state.foldToState(
  onLoading = { XxxState.Loading },
  onError = { message -> XxxState.Error(message, errorEventSink) },
  onContent = { items, isRefreshing -> XxxState.Success(items, isRefreshing, successEventSink) },
)
```

`ContentState.data` holds the last loaded value, which is what makes a refresh render as
`Success(isRefreshing = true)` rather than dropping back to `Loading`. Do not add a `retain`ed var
beside it to do that job — that pattern predates `ContentState` and is gone.

Inside `foldToState`, **`hasAnswer` is the discriminator, not `data.isEmpty()`** — see
`.claude/rules/data-flow.md`.

### Screen persistence

Nothing to register by hand: codegen turns each `@CircuitSerializable` screen into a saver
registration. The runtime failure modes, and why `SubScreen`s get no annotation, are in
`.claude/rules/circuit.md` → *Navigation persistence*.

## 2. Producer — `domain/.../producer/XxxProducer.kt`

Only if the presenter reads a repository. Thin `@Inject class` wrapping the repository flow in
`produceRetainedContentState` (from `libs.uistateCircuit`, shared with the `Countries` project), passing
`retryTrigger` plus whatever else the stream is keyed on. Model on
`domain/.../producer/CategoriesProducer.kt`.

For a source whose parameters change *while* it is on screen — a search term, a filter — reach for
`params.produceRetainedContentState(initial, keys) { p -> … }` instead: it cancels the in-flight request
per new parameter and marks the state reloading first, so content stays put under a refresh
indicator. Nothing here needs it yet.

## 3. Composable — `ui/.../<feature>/XxxScreen.kt`

```kotlin
@CircuitInject(XxxScreen::class, AppScope::class)
@Composable
fun XxxScreen(state: XxxState, modifier: Modifier = Modifier) { /* ... */ }
```

Same `@CircuitInject` arguments as the presenter — that pairing is the entire binding. The
composable imports `XxxState` / `XxxEvent` / `XxxScreen` from `:domain`; `:domain` never imports
from `:ui`. Reuse the shared helpers already in `:ui` (`ErrorDisplay`, `rememberAdaptiveGridCells`,
`rememberAdaptivePadding`) rather than rebuilding them.

## 4. URL mapping — `domain/.../navigation/ScreenUrlMapper.kt`

Add the screen to **both** directions, `Screen.toUrlPath()` and `urlPathToScreen()`, and update the
path table in the `toUrlPath` KDoc. This drives `recipes://app/...` deep links on Android and
browser history on web. Use `encodeURLPathPart()` / `decodeURLPart()` for any interpolated segment.

Screens with no public URL (e.g. `RecipeScaffoldScreen`) correctly fall through to `null` — leave them out.

## 5. Test — `domain/src/commonTest/.../XxxPresenterTest.kt`

testBalloon's declarative DSL plus Circuit's test helpers. Model on `CategoriesPresenterTest.kt`:
a private `FakeXxxRepository` with overridable handler lambdas, a `testFixture { }` building the
presenter with a `FakeNavigator`, and `presenter.test { awaitItem() }` assertions.

Also add the new screen to `ScreenSerializationTest.kt` (one `subclass(...)` line plus an entry in
`roundTripScreens`), next to its `ScreenUrlMapperTest.kt` entry.

## 6. Check it

The module needs `metro { enableCircuitCodegen = true }` — already set for `:domain`, `:ui`, and
`:shared`. Then run `/verify`; codegen errors surface as missing `Presenter.Factory` bindings at
compile time, not at runtime.
