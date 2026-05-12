package com.scottolcott.recipe.ui.recipe

import androidx.compose.animation.animateBounds
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalFlexBoxApi
import androidx.compose.foundation.layout.ExperimentalGridApi
import androidx.compose.foundation.layout.FlexBox
import androidx.compose.foundation.layout.Grid
import androidx.compose.foundation.layout.GridConfigurationScope
import androidx.compose.foundation.layout.GridScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXTRA_LARGE_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_LARGE_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import com.scottolcott.recipe.LocalWindowSizeClass
import com.scottolcott.recipe.domain.presenter.RecipeDetailsEvent
import com.scottolcott.recipe.domain.presenter.RecipeDetailsScreen
import com.scottolcott.recipe.domain.presenter.RecipeDetailsState
import com.scottolcott.recipe.model.Ingredient
import com.scottolcott.recipe.model.Recipe
import com.scottolcott.recipe.model.RecipeDetails
import com.scottolcott.recipe.model.RecipeId
import com.scottolcott.recipe.ui.Res
import com.scottolcott.recipe.ui.ThemeWrapper
import com.scottolcott.recipe.ui.an_error_occurred
import com.scottolcott.recipe.ui.image_24px
import com.scottolcott.recipe.ui.label_24px
import com.scottolcott.recipe.ui.link_24px
import com.scottolcott.recipe.ui.location_on_24px
import com.scottolcott.recipe.ui.rememberAdaptivePadding
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.zacsweers.metro.AppScope
import kotlin.time.Clock
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
@CircuitInject(RecipeDetailsScreen::class, AppScope::class)
fun RecipeDetailsScreen(state: RecipeDetailsState, modifier: Modifier = Modifier) {
  Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    val recipe = state.recipe
    if (state.loading) {
      CircularProgressIndicator()
    } else if (state.error) {
      Text(stringResource(Res.string.an_error_occurred))
    } else {
      recipe?.let { RecipeDetails(it, state.eventSink) }
    }
  }
}

@Composable
private fun RecipeDetails(recipe: Recipe, eventSink: (RecipeDetailsEvent) -> Unit) {
  val windowSizeClass = LocalWindowSizeClass.current
  val columns =
    when {
      windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_LARGE_LOWER_BOUND) ||
        windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_EXTRA_LARGE_LOWER_BOUND) -> 3
      windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND) -> 2
      else -> 1
    }

  val padding = rememberAdaptivePadding()

  LookaheadScope {
    SelectionContainer {
      Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding)) {
        RecipeTitle(recipe, Modifier.padding(bottom = 16.dp))
        RecipeGrid(recipe, columns, padding, eventSink)
      }
    }
  }
}

@OptIn(ExperimentalGridApi::class)
@Composable
private fun LookaheadScope.RecipeGrid(
  recipe: Recipe,
  columns: Int,
  padding: PaddingValues,
  eventSink: (RecipeDetailsEvent) -> Unit,
) {
  val recipeImage = remember {
    movableContentOf { modifier: Modifier ->
      RecipeImage(
        recipe,
        recipe.favorite,
        onToggleFavorite = { eventSink(RecipeDetailsEvent.ToggleFavorite) },
        modifier = modifier.animateBounds(this@RecipeGrid),
      )
    }
  }

  val ingredients = remember {
    movableContentOf { modifier: Modifier ->
      RecipeIngredients(recipe, modifier.animateBounds(this@RecipeGrid))
    }
  }

  val instructions = remember {
    movableContentOf { modifier: Modifier ->
      RecipeInstructions(recipe, modifier.animateBounds(this@RecipeGrid))
    }
  }
  val metaInfo = remember {
    movableContentOf { modifier: Modifier ->
      RecipeMetaInfo(recipe, eventSink, modifier.animateBounds(this@RecipeGrid))
    }
  }

  Grid(config = { recipeGridConfig(columns, padding) }, modifier = Modifier.fillMaxWidth()) {
    RecipeGridLayout(columns, recipeImage, ingredients, instructions, metaInfo)
  }
}

@OptIn(ExperimentalGridApi::class)
private fun GridConfigurationScope.recipeGridConfig(columns: Int, padding: PaddingValues) {
  when (columns) {
    1 -> column(1.fr)
    2 -> {
      column(1.fr)
      column(3.fr)
    }
    else -> {
      column(1.fr)
      column(3.fr)
      column(7.fr)
    }
  }
  gap(padding.calculateLeftPadding(LayoutDirection.Ltr))
}

@Suppress("ContentSlotReused")
@OptIn(ExperimentalGridApi::class)
@Composable
private fun GridScope.RecipeGridLayout(
  columns: Int,
  recipeImage: @Composable (Modifier) -> Unit,
  ingredients: @Composable (Modifier) -> Unit,
  instructions: @Composable (Modifier) -> Unit,
  metaInfo: @Composable (Modifier) -> Unit,
) {
  when (columns) {
    1 -> {
      recipeImage(Modifier.gridItem(row = 1, column = 1))
      ingredients(Modifier.gridItem(row = 2, column = 1))
      instructions(Modifier.gridItem(row = 3, column = 1))
      metaInfo(Modifier.gridItem(row = 4, column = 1))
    }
    2 -> {
      Column(
        Modifier.gridItem(row = 1, column = 1),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        recipeImage(Modifier)
        metaInfo(Modifier)
      }
      Column(
        Modifier.gridItem(row = 1, column = 2),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        ingredients(Modifier)
        instructions(Modifier)
      }
    }
    else -> {
      Column(
        Modifier.gridItem(row = 1, column = 1),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        recipeImage(Modifier)
        metaInfo(Modifier)
      }
      ingredients(Modifier.gridItem(row = 1, column = 2))
      instructions(Modifier.gridItem(row = 1, column = 3))
    }
  }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun RecipeTitle(recipe: Recipe, modifier: Modifier = Modifier) {
  Text(
    recipe.name,
    style = MaterialTheme.typography.headlineMediumEmphasized,
    modifier = modifier.fillMaxWidth(),
  )
}

@Composable
private fun RecipeMetaInfo(
  recipe: Recipe,
  eventSink: (RecipeDetailsEvent) -> Unit,
  modifier: Modifier = Modifier,
) {
  val details = recipe.details
  val uriHandler = LocalUriHandler.current

  Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
    RecipeTags(details?.tags.orEmpty())
    RecipeCategoryAndArea(recipe, eventSink)
    RecipeSources(details, uriHandler)
  }
}

@OptIn(ExperimentalFlexBoxApi::class)
@Composable
private fun RecipeTags(tags: List<String>) {
  if (tags.isNotEmpty()) {
    FlexBox(config = { gap(8.dp) }) {
      for (tag in tags) {
        AssistChip(onClick = {}, label = { Text(tag) })
      }
    }
  }
}

@Composable
private fun RecipeCategoryAndArea(recipe: Recipe, eventSink: (RecipeDetailsEvent) -> Unit) {
  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    recipe.category?.let { category ->
      AssistChip(
        onClick = { eventSink(RecipeDetailsEvent.CategoryClicked(category)) },
        label = { Text(category) },
        leadingIcon = { Icon(painterResource(Res.drawable.label_24px), null) },
        colors =
          AssistChipDefaults.assistChipColors(
            labelColor = MaterialTheme.colorScheme.primary,
            leadingIconContentColor = MaterialTheme.colorScheme.tertiary,
          ),
        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand, true),
      )
    }
    recipe.area?.let { area ->
      AssistChip(
        onClick = { eventSink(RecipeDetailsEvent.AreaClicked(area)) },
        label = { Text(area) },
        leadingIcon = { Icon(painterResource(Res.drawable.location_on_24px), null) },
        colors =
          AssistChipDefaults.assistChipColors(
            labelColor = MaterialTheme.colorScheme.primary,
            leadingIconContentColor = MaterialTheme.colorScheme.tertiary,
          ),
        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand, true),
      )
    }
  }
}

@Suppress("UnusedReceiverParameter")
@Composable
private fun ColumnScope.RecipeSources(details: RecipeDetails?, uriHandler: UriHandler) {
  val source = details?.source
  if (!source.isNullOrBlank()) {
    AssistChip(
      modifier = Modifier.pointerHoverIcon(PointerIcon.Hand, true),
      onClick = { uriHandler.openUri(source) },
      label = { Text(source) },
      leadingIcon = { Icon(painterResource(Res.drawable.link_24px), null) },
      colors =
        AssistChipDefaults.assistChipColors(
          labelColor = MaterialTheme.colorScheme.primary,
          leadingIconContentColor = MaterialTheme.colorScheme.tertiary,
        ),
    )
  }
  val imageSource = details?.imageSource
  if (!imageSource.isNullOrBlank()) {
    AssistChip(
      modifier = Modifier.pointerHoverIcon(PointerIcon.Hand, true),
      onClick = { uriHandler.openUri(imageSource) },
      label = { Text(imageSource) },
      leadingIcon = { Icon(painterResource(Res.drawable.image_24px), null) },
      colors =
        AssistChipDefaults.assistChipColors(
          labelColor = MaterialTheme.colorScheme.secondary,
          leadingIconContentColor = MaterialTheme.colorScheme.tertiary,
        ),
    )
  }
}

@Composable
private fun RecipeImage(
  recipe: Recipe,
  isFavorite: Boolean,
  onToggleFavorite: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier, contentAlignment = Alignment.BottomEnd) {
    AsyncImage(
      recipe.thumbnail,
      contentDescription = null,
      SingletonImageLoader.get(LocalPlatformContext.current),
      contentScale = ContentScale.Fit,
      modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(MaterialTheme.shapes.medium),
    )
    FavoriteIcon(
      isFavorite,
      modifier = Modifier.padding(12.dp).size(48.dp).pointerHoverIcon(PointerIcon.Hand, true),
      onClick = onToggleFavorite,
    )
  }
}

@PreviewWrapper(ThemeWrapper::class)
@Preview
@PreviewScreenSizes
@PreviewLightDark
@Composable
@Suppress("UnusedPrivateFunction")
private fun RecipeDetailsPreview() {
  val details =
    RecipeDetails(
      alternateName = null,
      instructions =
        @Suppress("MaxLineLength")
        """
        |Preheat oven to 350° F. Spray a 9x13-inch baking pan with non-stick spray.
        |            
        |            Combine soy sauce, ½ cup water, brown sugar, ginger and garlic in a small saucepan and cover. Bring to a boil over medium heat. Remove lid and cook for one minute once boiling.
        |            
        |            Meanwhile, stir together the corn starch and 2 tablespoons of water in a separate dish until smooth. Once sauce is boiling, add mixture to the saucepan and stir to combine. Cook until the sauce starts to thicken then remove from heat.
        |            
        |            Place the chicken breasts in the prepared pan. Pour one cup of the sauce over top of chicken. Place chicken in oven and bake 35 minutes or until cooked through. Remove from oven and shred chicken in the dish using two forks.
        |            
        |            *Meanwhile, steam or cook the vegetables according to package directions.
        |            
        |            Add the cooked vegetables and rice to the casserole dish with the chicken. Add most of the remaining sauce, reserving a bit to drizzle over the top when serving. Gently toss everything together in the casserole dish until combined. Return to oven and cook 15 minutes. Remove from oven and let stand 5 minutes before serving. Drizzle each serving with remaining sauce. Enjoy!
        """
          .trimMargin(),
      tags = listOf("Meat", "Casserole"),
      youtube = "https://www.youtube.com/watch?v=4aZr5hZXP_s",
      source = null,
      imageSource = null,
      creativeCommonsConfirmed = null,
      dateModified = null,
      ingredients =
        listOf(
          Ingredient("soy sauce", "3/4 cup"),
          Ingredient("water", "1/2 cup"),
          Ingredient("brown sugar", "1/4 cup"),
          Ingredient("ground ginger", "1/2 teaspoon"),
          Ingredient("minced garlic", "1/2 teaspoon"),
          Ingredient("cornstarch", "4 Tablespoons"),
          Ingredient("chicken breasts", "2"),
          Ingredient("stir-fry vegetables", "1 (12 oz.)"),
          Ingredient("brown rice", "3 cups"),
        ),
      lastFetched = Clock.System.now(),
    )
  val recipe =
    Recipe(
      id = RecipeId("52772"),
      name = "Teriyaki Chicken Casserole",
      thumbnail = "https://www.themealdb.com/images/media/meals/wvpsxx1468256321.jpg",
      category = "Chicken",
      area = "Japanese",
      favorite = true,
      details = details,
      lastFetched = Clock.System.now(),
    )
  RecipeDetails(recipe) {}
}
