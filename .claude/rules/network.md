---
paths:
  - "network/**"
  - "repository/**"
---

# TheMealDB network layer

Endpoint table, tier limits, image URLs, and attribution: `/themealdb-api`.

## Base URL and key

This app uses TheMealDB **v2 exclusively**; there is no v1 fallback.

```
https://www.themealdb.com/api/json/v2/{MEALDB_API_KEY}/
```

Built in exactly one place: `network/.../NetworkProviders.kt` (`MEALDB_BASE_URL` + `DefaultRequest`).
Every `@Resource` path is relative to that trailing slash, so no resource declares a version.

`MEALDB_API_KEY` is **required**. It flows Gradle property or env var →
`build-logic/.../ProjectExt.kt` → BuildKonfig `SharedBuildConfig.MEALDB_API_KEY` (`shared/build.gradle.kts`)
→ each platform's `RuntimeConfigImpl` → `RuntimeConfig.mealDbApiKey: String` (non-null). A missing or
blank key fails the Gradle build with a message pointing at the signup page.

**The key is in the URL path, not a header.** No `LogLevel` short of `NONE` omits the request URL, so
`NetworkProviders.redacting()` wraps the Ktor logger and rewrites the key to `***`. Preserve that
wrapper if you touch the logging block, and never add a second client that logs API URLs unredacted.

**Why v2 only:** with a real key, v2 returns everything v1 does plus more results and four extra
endpoints. But **v2 with the free dev key silently caps `filter.php` at one result**, which would
reduce every browse-by-category screen to a single recipe and look like an app bug rather than a
tier limit. Hence: one base URL, and a build that fails without a key rather than shipping that.
If a filter suddenly returns exactly one meal, suspect the key before suspecting the code.

Two `Json` qualifiers exist, `@NetworkJson` and `@StorageJson`
(`core/.../serialization/JsonQualifiers.kt`). Pick deliberately.

## Resources

New resources mirror an existing sibling in `network/.../resource/` (all are `internal class`, not
`data class`) and its `api/` consumer. Spaces in `c` / `a` / `i` values may be written as
underscores (`chicken_breast`). URL-encode anything user-supplied.

**`list.php?a=list` and `filter.php?a=` do not agree on what an area is.** The list returns ~195
rows with a real key, but `filter.php` matches the exact `strArea` string meals actually carry —
14 distinct values on v1. Most rows in the area list therefore have no recipes behind them.
`RecipeApiImpl.getByArea` asks for both spellings the list gives (`Italian` and `Italy`) and merges
the results, which recovers some but not all of the gap. If the Areas tab looks full of dead ends,
this is why — not a bug in the screen.

## Response shapes

Top-level wrapper is always `{"meals": ...}` — except `categories.php`, which uses `{"categories": [...]}`.

**`filter.php` returns summaries, everything else returns full records.** Modelled as two DTOs in
`network/.../dto/RecipeDetailsDto.kt`:

- `RecipeBasicDto` — `idMeal`, `strMeal`, `strMealThumb` only.
- `RecipeFullDto` — adds category, area, instructions, youtube, source, tags, and the ingredients.

**Never present a filter result as a complete recipe.** Filter to get candidates, then `lookup.php?i=`
each `idMeal` for the real ingredients and instructions. This is TheMealDB's own documented workflow,
and it is also just true of the data — a summary has no instructions to show.

## DTO field traps

- **Ingredients are flat, not a list.** `strIngredient1..20` paired with `strMeasure1..20` by index.
  Pair by `N`, keep order, trim, skip null/empty ingredient slots. **A measure can be blank while its
  ingredient exists** — don't drop the ingredient. Already handled in `RecipeFullDto`.
- **`{"meals": null}` means not found**, not an error. Expect it from any search or filter.
- **`meals` is not always an array.** The v2 schema allows a string or object in that slot for
  access-level messages. `Json { ignoreUnknownKeys = true }` does **not** save you here — it tolerates
  unknown *keys*, not a changed *type*, so such a response throws.
- **`idMeal` is typed `integer | string`** in the spec. It arrives as a string in practice; the repo
  wraps it as `RecipeId`.
- **`strThumb` on ingredients is a lie.** `IngredientDto` declares it, but `list.php?i=list` does not
  return it. Build ingredient images from the name instead (see `/themealdb-api` → Images).
- **`strCountry` on the area list is nullable** and absent on some rows, so `AreaDto.country` is
  `String?`. A non-null declaration fails the decode of the *entire* list on one bad row.
- Nullable in practice: `strArea`, `strCategory`, `strTags`, `strYoutube`, `strSource`,
  `strImageSource`, `strCreativeCommonsConfirmed`, `dateModified`.
