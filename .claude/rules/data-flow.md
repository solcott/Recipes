---
paths:
  - "repository/**"
  - "domain/**"
---

# Reading data: `Outcome` and `ContentState`

Repositories return `Flow<Outcome<T>>`, not Store's own `StoreReadResponse`. **Store 5 is an
implementation detail of `:repository`** — it is an `implementation` dependency there, and nothing
above that module compiles against it. `io.github.solcott:dataresult-store5` does the translation
in `asOutcomes(fetching = refresh) { … }`, at the end of each repository's chain. It sits *inside*
the `flatMapLatest { refresh -> … }` so it can see whether a fetch is coming.

The types come from [kmp-dataresult](https://github.com/solcott/kmp-dataresult), shared with the
`Countries` project. Resolving them needs `gpr.user`/`gpr.key` in `~/.gradle/gradle.properties` (a
classic PAT with `read:packages`) — GitHub Packages authenticates even public reads.

The path from a repository to a screen state is three steps:

1. `Flow<Outcome<T>>` — `Loading`, `Data(value, origin)` or `Error(cause, origin)`. Store's
   `Initial` and `Loading` both become `Outcome.Loading`; `NoNewData` is dropped. An empty first
   read from cache while a fetch is pending is held back until the fetch answers. Room returns `[]`
   for a key it has never fetched, and that's a cache miss, not an empty result.
   `getFavoritesAsFlow` passes no `fetching`, because nothing is coming to answer instead.
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

- **Check `hasAnswer`, never `data.isEmpty()`**, to tell "nothing has loaded yet" from "loaded and
  empty". `hasLoaded` reads `origin`, which stays null until the first value arrives. `hasAnswer`
  also refuses an empty read *from cache* while its request is in flight or has failed. That's a key
  the database has never seen, not an empty result. The repositories already hold that read back
  with `asOutcomes(fetching = refresh) { … }`, and `hasAnswer` is the backstop. Testing the data for
  emptiness leaves a spinner over a legitimately empty tab forever. `hasAnswer` can't, because it is
  only false while a request is outstanding or has failed.
- **Having data beats being in flight.** `foldToState` checks `hasAnswer` first, which is what
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
