---
name: themealdb-api
description: TheMealDB API reference for this repo — v2 base URL and key handling, every endpoint and which @Resource maps to it, free-vs-premium tier limits, response shapes and DTO field traps, image URL construction, and attribution rules. Use when adding or changing a network call, debugging an empty or truncated API response, mapping a DTO, or building an image URL.
---

# TheMealDB API

This app uses **v2 exclusively**. The v1 fallback was removed — see [Why v2 only](#why-v2-only).

## Base URL and key

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

## Endpoints

| Endpoint | Params | Returns | `@Resource` in `network/.../resource/` |
|---|---|---|---|
| `search.php` | `s` name (partial ok) | full meals | `SearchResource` |
| `search.php` | `f` single letter | full meals | — not wired |
| `lookup.php` | `i` meal id | full meal | `LookupResource` |
| `random.php` | — | 1 full meal | `RandomResource` |
| `filter.php` | `c` category | **summaries** | `FilterResource(c=)` |
| `filter.php` | `a` area | **summaries** | `FilterResource(a=)` |
| `filter.php` | `i` ingredient(s) | **summaries** | — not wired |
| `categories.php` | — | category records | `CategoryResource` |
| `list.php` | `i=list` | full ingredient records | `IngredientsResource` |
| `list.php` | `c=list` / `a=list` | bare category / area names | — not wired |
| `popular.php` | — | full meals (20) | — not wired |
| `latest.php` | — | full meals (10) | — not wired |
| `randomselection.php` | — | full meals (10) | — not wired |

"Not wired" means the endpoint works with our key today but has no resource, API method, or
repository. Adding one is a normal change — mirror an existing sibling in `resource/` (all are
`internal class`, not `data class`) and its `api/` consumer.

Spaces in `c` / `a` / `i` values may be written as underscores (`chicken_breast`). URL-encode
anything user-supplied. `filter.php?i=` accepts **up to four** comma-separated ingredients on v2.

Full spec: `reference/openapi-v2.yaml` (vendored 2026-08-26).

## Tier limits

Measured against the live API, dev key `1` vs a premium key. This is the non-obvious part:

| Call | v1 (`/v1/1/`) | v2 + dev key `1` | v2 + premium key |
|---|---|---|---|
| `filter.php?c=Seafood` | 84 | **1** | 84 |
| `filter.php?a=Canadian` | 22 | **1** | 22 |
| `filter.php?i=chicken_breast` | 17 | **1** | 17 |
| `filter.php?i=a,b` (multi) | `null` | 1 | 11 |
| `search.php?s=chicken` | 25 | 25 | **64** |
| `list.php?a=list` | 14 | 14 | **195** |
| `popular.php` | 404 | 20 | 20 |
| `latest.php` / `randomselection.php` | Patreon message | 1 | 10 |

### Why v2 only

With a real key, v2 returns everything v1 does plus more results and four extra endpoints — v1 has
no advantage. But **v2 with the free dev key silently caps `filter.php` at one result**, which would
reduce every browse-by-category screen to a single recipe and look like an app bug rather than a
tier limit. Hence: one base URL, and a build that fails without a key rather than shipping that.

If a filter suddenly returns exactly one meal, suspect the key before suspecting the code.

## Response shapes

Top-level wrapper is always `{"meals": ...}` — except `categories.php`, which uses `{"categories": [...]}`.

**`filter.php` returns summaries, everything else returns full records.** Modelled as two DTOs in
`network/.../dto/RecipeDetailsDto.kt`:

- `RecipeBasicDto` — `idMeal`, `strMeal`, `strMealThumb` only.
- `RecipeFullDto` — adds category, area, instructions, youtube, source, tags, and the ingredients.

**Never present a filter result as a complete recipe.** Filter to get candidates, then `lookup.php?i=`
each `idMeal` for the real ingredients and instructions. This is TheMealDB's own documented workflow,
and it is also just true of the data — a summary has no instructions to show.

### Field traps

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
  return it. Build ingredient images from the name instead (below).
- Nullable in practice: `strArea`, `strCategory`, `strTags`, `strYoutube`, `strSource`,
  `strImageSource`, `strCreativeCommonsConfirmed`, `dateModified`.

## Images

Meal thumbnails support size suffixes appended to the returned `strMealThumb` — useful for grids,
and unused in this repo today:

```
{strMealThumb}          full size
{strMealThumb}/small    200 x 200
{strMealThumb}/medium   350 x 350
{strMealThumb}/large    500 x 500
```

Ingredient art is keyed by name, with the same suffixes (URL-encode the name):

```
https://www.themealdb.com/images/ingredients/Chicken.png
https://www.themealdb.com/images/ingredients/Chicken.png/small
```

The full-size ingredient PNG is ~575 KB versus ~57 KB for `/small` — prefer a suffix in list UI.
Images are fetched with the separate `@CoilClient` Ktor client, which has no base URL and no key.

## Attribution and responsible use

- Credit TheMealDB as the source of recipe data and imagery in anything user-facing.
- Preserve `strSource`, `strImageSource`, and `strCreativeCommonsConfirmed` when present.
- **Never infer that a recipe is allergen-free, vegetarian, vegan, halal, kosher, or medically
  suitable from its name, tags, category, or area.** TheMealDB is a recipe database, not an allergen
  certification service. Don't invent quantities, substitute ingredients silently, or add cooking
  temperatures and storage times the recipe does not state.
- Use the API; never scrape the site.

## Upstream

- Docs: https://www.themealdb.com/documentation
- Agent guide: https://www.themealdb.com/AGENTS.md
- Agent skill: https://www.themealdb.com/SKILL.md
- OpenAPI v2: https://www.themealdb.com/api/spec/openapi-v2.yaml (vendored in `reference/`)
- OpenAPI v1: https://www.themealdb.com/api/spec/openapi-v1.yaml (not used by this app)
- Terms: https://www.themealdb.com/terms_of_use.php
