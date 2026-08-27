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

@Parcelize data object XxxScreen : Screen
```

Rules that are easy to get wrong:

- **`@Redacted` on every `eventSink`.** Comes from `dev.zacsweers.redacted.annotations.Redacted`;
  keeps lambdas out of `toString`.
- **Events are nested per state**, not one flat sealed interface. A `Success` state's sink only
  accepts `XxxEvent.Success`.
- **`retain { }`**, from `androidx.compose.runtime.retain`, for state that must survive
  config change — never `rememberSaveable`.
- **`@Parcelize` is `io.github.solcott.kmp.parcelize.Parcelize`**, not kotlin-parcelize.
- A retry is conventionally an `Int` counter (`retryTrigger`) passed into the producer as a key,
  incremented by `XxxEvent.Error.RetryClicked`.
- Screens with parameters are a sealed interface with `@Parcelize data class` cases — see
  `RecipesScreen.ByCategory` / `.ByArea` / `.BySearch` / `.Favorites`.

### Handling `StoreReadResponse`

Repositories return `Flow<StoreReadResponse<T>>`, and the `when` must be exhaustive over
`Initial`, `Loading`, `NoNewData`, `Data`, `Error.Exception`, `Error.Message`, `Error.Custom<*>`.
The established pattern keeps the last successful value in a `retain`ed var so refreshes show
`Success(isRefreshing = true)` instead of dropping back to `Loading`.

## 2. Producer — `domain/.../producer/XxxProducer.kt`

Only if the presenter reads a repository. Thin `@Inject class` wrapping the repository flow in
`produceRetainedState`, keyed on `retryTrigger`, dropping `NoNewData`. Model on
`domain/.../producer/CategoriesProducer.kt`.

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

## 6. Check it

The module needs `metro { enableCircuitCodegen = true }` — already set for `:domain`, `:ui`, and
`:shared`. Then run `/verify`; codegen errors surface as missing `Presenter.Factory` bindings at
compile time, not at runtime.
