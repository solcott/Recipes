package com.scottolcott.recipe.ui

import androidx.compose.runtime.Composable
import com.scottolcott.recipe.domain.presenter.AreasScreen
import com.scottolcott.recipe.domain.presenter.CategoriesScreen
import com.scottolcott.recipe.domain.presenter.HomeScreen
import com.scottolcott.recipe.domain.presenter.IngredientsScreen
import com.scottolcott.recipe.domain.presenter.RecipeDetailsScreen
import com.scottolcott.recipe.domain.presenter.RecipesScreen
import com.slack.circuit.runtime.screen.Screen
import org.jetbrains.compose.resources.stringResource

/**
 * The human-readable name of a [Screen], or `null` for one with nothing to call it.
 *
 * The `when` covers the same screens as `ScreenUrlMapper.toUrlPath()`, and for the same reason: a
 * screen the user can navigate to needs a name in both places. It lives in `:ui` rather than beside
 * that mapper because it resolves `Res.string` — a presentation concern, and `:domain` carries no
 * compose resources.
 *
 * Used for the recipes list's own heading and for the label on the back button, which names the
 * screen the user is going *back to* rather than the one they are on. The top app bar carries no
 * title — a screen names itself in its own content, where the name survives the search bar taking
 * the whole bar over on wide layouts.
 *
 * The tab branch is left exhaustive on purpose: adding a `HomeTabScreen` should fail to compile
 * here until it is given a name, the same way it must be added to `HOME_TABS` to be addressable.
 *
 * [RecipeDetailsScreen] is the one screen whose real title — the recipe's name — is not reachable
 * from the [Screen], which carries only a `RecipeId`. It falls back to the app name, which nothing
 * currently renders: the recipe's own headline names that screen, and a back label pointing at a
 * recipe would need something in the graph to push on top of one.
 */
@Composable
fun Screen.title(): String? =
  when (this) {
    is HomeScreen ->
      when (tab) {
        CategoriesScreen -> stringResource(Res.string.categories)
        IngredientsScreen -> stringResource(Res.string.ingredients)
        AreasScreen -> stringResource(Res.string.areas)
      }
    is RecipesScreen.ByCategory -> stringResource(Res.string.category, category)
    is RecipesScreen.ByArea -> stringResource(Res.string.area, area)
    is RecipesScreen.BySearch -> stringResource(Res.string.search_results, searchTerm)
    // Sorted for the same reason the URL mapper sorts: one set has exactly one rendering.
    is RecipesScreen.ByIngredient ->
      stringResource(Res.string.ingredient, ingredients.sorted().joinToString(", "))
    is RecipesScreen.Favorites -> stringResource(Res.string.favorites)
    is RecipeDetailsScreen -> stringResource(Res.string.recipes)
    else -> null
  }
