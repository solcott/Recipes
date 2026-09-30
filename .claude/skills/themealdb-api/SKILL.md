---
name: themealdb-api
description: TheMealDB API reference for this repo — every endpoint and which @Resource maps to it, free-vs-premium tier limits, image URL construction, and attribution rules. Use when adding a network call, choosing an endpoint, debugging an empty or truncated API response, building an image URL, or writing user-facing copy about recipe data. DTO field traps and key handling are in .claude/rules/network.md.
---

# TheMealDB API

On-demand reference for the endpoint map, tier limits, images, and attribution. The invariants —
base URL and key handling, why v2 only, response shapes, and DTO field traps — are in
`.claude/rules/network.md`, which loads whenever you touch `:network` or `:repository`.

Base URL: `https://www.themealdb.com/api/json/v2/{MEALDB_API_KEY}/` (v2 only, key in the path).

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
| `list.php` | `a=list` | area records (`strArea` + `strCountry`) | `AreasResource` |
| `list.php` | `c=list` | bare category names | — not wired |
| `popular.php` | — | full meals (20) | — not wired |
| `latest.php` | — | full meals (10) | — not wired |
| `randomselection.php` | — | full meals (10) | — not wired |

"Not wired" means the endpoint works with our key today but has no resource, API method, or
repository. Adding one is a normal change — mirror an existing sibling in `resource/` (all are
`internal class`, not `data class`) and its `api/` consumer.

Spaces in `c` / `a` / `i` values may be written as underscores (`chicken_breast`). URL-encode
anything user-supplied. `filter.php?i=` accepts **up to four** comma-separated ingredients on v2.

`list.php?a=list` and `filter.php?a=` disagree on what an area is — see `.claude/rules/network.md`.

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

Why that forces v2 with a real key: `.claude/rules/network.md` → *Why v2 only*.

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
