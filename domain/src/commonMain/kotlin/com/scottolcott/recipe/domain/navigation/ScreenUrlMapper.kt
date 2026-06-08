package com.scottolcott.recipe.domain.navigation

import androidx.compose.runtime.staticCompositionLocalOf
import com.scottolcott.recipe.domain.presenter.CategoriesScreen
import com.scottolcott.recipe.domain.presenter.RecipeDetailsScreen
import com.scottolcott.recipe.domain.presenter.RecipesScreen
import com.scottolcott.recipe.model.RecipeId
import com.slack.circuit.runtime.screen.Screen
import io.ktor.http.decodeURLPart
import io.ktor.http.encodeURLPathPart

/**
 * Converts a [Screen] to its canonical URL path, or `null` for internal screens (e.g.
 * [RecipeScaffoldScreen]) that have no public URL representation.
 *
 * | Screen                         | Path                     |
 * |--------------------------------|--------------------------|
 * | CategoriesScreen               | /home                    |
 * | RecipesScreen.ByCategory(name) | /recipes/category/{name} |
 * | RecipesScreen.ByArea(name)     | /recipes/area/{name}     |
 * | RecipesScreen.BySearch(term)   | /recipes/search/{term}   |
 * | RecipesScreen.Favorites        | /recipes/favorites       |
 * | RecipeDetailsScreen(id)        | /recipe/{id}             |
 */
fun Screen.toUrlPath(): String? =
  when (this) {
    is CategoriesScreen -> "/home"
    is RecipesScreen.ByCategory -> "/recipes/category/${category.encodeURLPathPart()}"
    is RecipesScreen.ByArea -> "/recipes/area/${area.encodeURLPathPart()}"
    is RecipesScreen.BySearch -> "/recipes/search/${searchTerm.encodeURLPathPart()}"
    is RecipesScreen.Favorites -> "/recipes/favorites"
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
fun urlPathToScreen(rawPathOrUrl: String): Screen? {
  // Strip custom scheme + authority to obtain a plain path
  val path =
    when {
      rawPathOrUrl.startsWith("recipes://") -> {
        val withoutScheme = rawPathOrUrl.removePrefix("recipes://")
        val slashIndex = withoutScheme.indexOf('/')
        if (slashIndex >= 0) withoutScheme.substring(slashIndex) else "/"
      }
      else -> rawPathOrUrl
    }

  val normalized = path.trimEnd('/').let { if (it.isEmpty()) "/home" else it }

  return when {
    normalized == "/home" || normalized == "/" -> CategoriesScreen
    normalized == "/recipes/favorites" -> RecipesScreen.Favorites
    normalized.startsWith("/recipes/category/") ->
      normalized
        .removePrefix("/recipes/category/")
        .takeIf { it.isNotBlank() }
        ?.decodeURLPart()
        ?.let { RecipesScreen.ByCategory(it) }
    normalized.startsWith("/recipes/area/") ->
      normalized
        .removePrefix("/recipes/area/")
        .takeIf { it.isNotBlank() }
        ?.decodeURLPart()
        ?.let { RecipesScreen.ByArea(it) }
    normalized.startsWith("/recipes/search/") ->
      normalized
        .removePrefix("/recipes/search/")
        .takeIf { it.isNotBlank() }
        ?.decodeURLPart()
        ?.let { RecipesScreen.BySearch(it) }
    normalized.startsWith("/recipe/") ->
      normalized
        .removePrefix("/recipe/")
        .takeIf { it.isNotBlank() }
        ?.let { RecipeDetailsScreen(RecipeId(it)) }
    else -> null
  }
}

/**
 * Provides the initial deep-link [Screen] to [com.scottolcott.recipe.domain.presenter.RecipeScaffoldPresenter] so the inner nav stack is
 * seeded correctly on first composition.
 *
 * Provided by [RecipeApp] on every platform; defaults to `null` (no deep link).
 */
val LocalDeepLinkScreen = staticCompositionLocalOf<Screen?> { null }
