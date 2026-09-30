---
paths:
  - "domain/**"
  - "ui/**"
---

# Circuit screens, state, and navigation

A presenter (`:domain`) and its UI (`:ui`) are joined only by
`@CircuitInject(XxxScreen::class, AppScope::class)` on each side. `:domain` never imports from
`:ui`. For the step-by-step of adding a screen, use `/circuit-screen`.

## Presenter file layout

`domain/.../presenter/XxxPresenter.kt`, in this order: presenter class → sealed
`XxxState : CircuitUiState` (Loading/Error/Success) → sealed `XxxEvent` with nested per-state
sub-interfaces → the `Screen` last, as `@CircuitSerializable(AppScope::class) data object XxxScreen : Screen`.

- **`@Redacted` on every `eventSink`.** Comes from `dev.zacsweers.redacted.annotations.Redacted`;
  keeps lambdas out of `toString`.
- **Events are nested per state**, not one flat sealed interface. A `Success` state's sink only
  accepts `XxxEvent.Success`.
- A retry is conventionally an `Int` counter (`retryTrigger`) passed into the producer as a key,
  incremented by `XxxEvent.Error.RetryClicked`.

## Retained state

State survives recomposition/config change via `androidx.compose.runtime.retain.retain { }`,
**not** `rememberSaveable`. One sanctioned exception: `HomePresenter` persists the selected tab
with `rememberSerializable`, because that selection is cheap to store and worth surviving process
death so the app reopens on the tab the user left. Don't "fix" it to `retain { }`.

**`:domain` depends on Material3 (`api`), deliberately.** `SearchState` holds a `SearchBarState`
and `RecipeScaffoldState`/`SearchScreen` carry a `SearchBarValue`, so that the presenter — not the
composable — owns whether the search bar is expanded. This is the *only* sanctioned place for
Material3 widget state in `:domain`; other widget state belongs in `:ui`.

## URL mapping

A new screen must also be added to `domain/.../navigation/ScreenUrlMapper.kt` — both the
`Screen.toUrlPath()` and `urlPathToScreen()` directions. It backs `recipes://app/...` deep links
and browser history on web. Home tabs are the exception to writing anything there by hand: each
`HomeTabScreen` carries its own `urlSegment` and the mapper reads `HOME_TABS`, so adding a tab to
that one list makes it addressable in both directions.

## Navigation persistence

Since Circuit 0.38, `Screen` and `PopResult` are no longer `Parcelable`; the back stack is
persisted with kotlinx-serialization.

- **`@CircuitSerializable(AppScope::class)` on the `Screen`**, from
  `com.slack.circuit.serialization`. It is `@MetaSerializable`, so it already implies
  `@Serializable` — don't add that too.
- Screens with parameters are a sealed interface whose **concrete cases** carry the annotation —
  see `RecipesScreen.ByCategory` / `.ByArea` / `.BySearch` / `.Favorites`. The sealed parent is
  never annotated.
- Every parameter of a screen must be serializable. `RecipeId`, `CategoryId` and `IngredientId`
  already are; a new parameter type in `:model` needs `@Serializable` added there.
- `SubScreen`s from `circuitx-subcircuit` never enter the back stack and get **no** annotation —
  they may hold unserializable state such as a `TextFieldState`.

Circuit codegen turns each `@CircuitSerializable` screen into a `CircuitSerializerRegistration`
multibinding, and `domain/.../circuit/CircuitProviders.kt` folds the whole set into a
`SerializableCircuitSaver` on `Circuit.Builder`. Nothing to register by hand — but the failure
modes are runtime, not compile time:

- The saver is installed with the static `setCircuitSaver(saver)` overload rather than the
  `setCircuitSaver { fallback -> ... }` transform, so **saving** an unregistered or unserializable
  screen **throws** instead of quietly falling through to the registry-backed saver. That is
  deliberate.
- **Restoring** one can only return null, and the nav stack drops that record; `CircuitProviders`
  passes `onRestoreError` to the logger so it isn't silent.

**The nav stack is `rememberSaveableNavStack`, not `rememberSaveableBackStack`.** `BackStack`
stubs `forward()` and `backward()` to `false`, and `NavigatorImpl` delegates straight to them, so
switching to it silently turns the browser back/forward buttons on web into no-ops —
`ui/src/webMain/.../BrowserHistoryEffect.web.kt` drives navigation through
`Navigator.backward()`/`forward()`. Both `RecipeApp.kt` and `RecipeScaffoldPresenter.kt` must stay
on `rememberSaveableNavStack`.
