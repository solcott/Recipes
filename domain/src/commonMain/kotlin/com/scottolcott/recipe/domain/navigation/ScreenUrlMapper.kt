package com.scottolcott.recipe.domain.navigation

import androidx.compose.runtime.staticCompositionLocalOf
import com.scottolcott.recipe.domain.presenter.HomeScreen
import com.scottolcott.recipe.domain.presenter.RecipeDetailsScreen
import com.scottolcott.recipe.domain.presenter.RecipesScreen
import com.scottolcott.recipe.model.RecipeId
import com.slack.circuit.runtime.screen.Screen
import io.ktor.http.decodeURLPart
import io.ktor.http.encodeURLPathPart
import io.ktor.http.encodeURLQueryComponent

private const val DEEP_LINK_SCHEME = "recipes://"
private const val HOME_PATH = "/home"
private const val FAVORITES_PATH = "/recipes/favorites"

/** Separates the ingredient names packed into a single `/recipes/ingredient/` segment. */
private const val INGREDIENT_SEPARATOR = ","

/**
 * The routes that carry a trailing path segment, paired with the [Screen] that segment builds, or
 * `null` when the segment does not yield a usable screen.
 *
 * Each builder receives the segment still encoded and decodes it itself, so the multi-value
 * ingredient route can split before decoding rather than after.
 *
 * Order matters only in that a prefix must not shadow a longer one; the current prefixes are all
 * mutually exclusive.
 */
private val PARAMETERIZED_ROUTES: List<Pair<String, (String) -> Screen?>> =
  listOf(
    "/recipes/category/" to { raw -> RecipesScreen.ByCategory(raw.decodeURLPart()) },
    "/recipes/area/" to { raw -> RecipesScreen.ByArea(raw.decodeURLPart()) },
    "/recipes/search/" to { raw -> RecipesScreen.BySearch(raw.decodeURLPart()) },
    "/recipes/ingredient/" to { raw -> raw.toIngredientNames()?.let(RecipesScreen::ByIngredient) },
    "/recipe/" to { raw -> RecipeDetailsScreen(RecipeId(raw.decodeURLPart())) },
  )

/**
 * Splits an encoded `/recipes/ingredient/` segment into its names, or `null` if none survive.
 *
 * The split happens before decoding: [encodeIngredient] percent-encodes every character outside
 * `[A-Za-z0-9]`, so a bare [INGREDIENT_SEPARATOR] in the segment can only be a separator and never
 * part of a name.
 */
private fun String.toIngredientNames(): Set<String>? =
  split(INGREDIENT_SEPARATOR)
    .map { it.decodeURLPart() }
    .filter(String::isNotBlank)
    .toSet()
    .takeIf(Set<String>::isNotEmpty)

/**
 * Encodes one ingredient name for the packed segment.
 *
 * Deliberately not `encodeURLPathPart`, which leaves `,` untouched — it is a valid path character —
 * and would let a name containing a comma split into two on the way back.
 */
private fun String.encodeIngredient(): String = encodeURLQueryComponent(encodeFull = true)

/**
 * Converts a [Screen] to its canonical URL path, or `null` for internal screens (e.g.
 * [RecipeScaffoldScreen]) that have no public URL representation.
 *
 * | Screen                          | Path                        |
 * |---------------------------------|-----------------------------|
 * | CategoriesScreen                | /home                       |
 * | RecipesScreen.ByCategory(name)  | /recipes/category/{name}    |
 * | RecipesScreen.ByArea(name)      | /recipes/area/{name}        |
 * | RecipesScreen.BySearch(term)    | /recipes/search/{term}      |
 * | RecipesScreen.ByIngredient(set) | /recipes/ingredient/{a},{b} |
 * | RecipesScreen.Favorites         | /recipes/favorites          |
 * | RecipeDetailsScreen(id)         | /recipe/{id}                |
 */
fun Screen.toUrlPath(): String? =
  when (this) {
    is HomeScreen -> HOME_PATH
    is RecipesScreen.ByCategory -> "/recipes/category/${category.encodeURLPathPart()}"
    is RecipesScreen.ByArea -> "/recipes/area/${area.encodeURLPathPart()}"
    is RecipesScreen.BySearch -> "/recipes/search/${searchTerm.encodeURLPathPart()}"
    // Sorted so a set has exactly one URL, matching the canonical form RecipesKey.ByIngredient.of
    // builds for the cache key.
    is RecipesScreen.ByIngredient ->
      ingredients
        .sorted()
        .joinToString(INGREDIENT_SEPARATOR) { it.encodeIngredient() }
        .let { "/recipes/ingredient/$it" }
    is RecipesScreen.Favorites -> FAVORITES_PATH
    is RecipeDetailsScreen -> "/recipe/${id.id.encodeURLPathPart()}"
    else -> null
  }

/**
 * Parses a URL path or deep link URI into a [Screen], or `null` if unrecognised.
 *
 * Accepts:
 * - Plain paths: `/recipe/52772`
 * - Custom-scheme deep links: `recipes://app/recipe/52772`
 *
 * All platforms share the same parser so the URL scheme is consistent everywhere.
 */
fun urlPathToScreen(rawPathOrUrl: String): Screen? =
  when (val normalized = normalizePath(rawPathOrUrl)) {
    HOME_PATH -> HomeScreen
    FAVORITES_PATH -> RecipesScreen.Favorites
    else -> parameterizedScreen(normalized)
  }

/**
 * Strips the custom scheme and authority from [rawPathOrUrl], drops any trailing slash, and maps
 * the empty path onto [HOME_PATH].
 */
private fun normalizePath(rawPathOrUrl: String): String {
  val path =
    if (rawPathOrUrl.startsWith(DEEP_LINK_SCHEME)) {
      val withoutScheme = rawPathOrUrl.removePrefix(DEEP_LINK_SCHEME)
      val slashIndex = withoutScheme.indexOf('/')
      if (slashIndex >= 0) withoutScheme.substring(slashIndex) else "/"
    } else {
      rawPathOrUrl
    }
  return path.trimEnd('/').ifEmpty { HOME_PATH }
}

/** Matches [path] against [PARAMETERIZED_ROUTES], decoding the trailing segment. */
private fun parameterizedScreen(path: String): Screen? =
  PARAMETERIZED_ROUTES.firstNotNullOfOrNull { (prefix, toScreen) ->
    path
      .takeIf { it.startsWith(prefix) }
      ?.removePrefix(prefix)
      ?.takeIf(String::isNotBlank)
      ?.let(toScreen)
  }

/**
 * Provides the initial deep-link [Screen] to
 * [com.scottolcott.recipe.domain.presenter.RecipeScaffoldPresenter] so the inner nav stack is
 * seeded correctly on first composition.
 *
 * Provided by [RecipeApp] on every platform; defaults to `null` (no deep link).
 */
@Suppress("CompositionLocalAllowlist")
val LocalDeepLinkScreen = staticCompositionLocalOf<Screen?> { null }
