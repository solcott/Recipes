package com.scottolcott.recipe.ui.recipe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import com.scottolcott.recipe.domain.presenter.RecipesEvent
import com.scottolcott.recipe.domain.presenter.RecipesScreen
import com.scottolcott.recipe.domain.presenter.RecipesState
import com.scottolcott.recipe.model.Recipe
import com.scottolcott.recipe.ui.ErrorDisplay
import com.scottolcott.recipe.ui.Res
import com.scottolcott.recipe.ui.isShortWindow
import com.scottolcott.recipe.ui.no_recipes_found
import com.scottolcott.recipe.ui.rememberAdaptiveGridCells
import com.scottolcott.recipe.ui.rememberAdaptivePadding
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.zacsweers.metro.AppScope
import org.jetbrains.compose.resources.stringResource

@Suppress("unused")
@Composable
@CircuitInject(RecipesScreen::class, AppScope::class)
fun RecipesScreen(state: RecipesState, modifier: Modifier = Modifier) {
  val cells = rememberAdaptiveGridCells(targetWidth = 170.dp, shortWindowTargetWidth = 260.dp)
  val padding = rememberAdaptivePadding()
  // Read once here rather than per card: the cell width and the card design have to come from the
  // same answer, and a grid item is the wrong place to read a CompositionLocal.
  val horizontalCards = isShortWindow()
  when (state) {
    is RecipesState.Error ->
      Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ErrorDisplay(onRetryClick = { state.eventSink(RecipesEvent.Error.RetryClicked) })
      }
    RecipesState.Loading ->
      Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
      }
    is RecipesState.Success -> {
      if (state.recipes.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text(stringResource(Res.string.no_recipes_found))
        }
      } else {
        LazyVerticalGrid(
          cells,
          modifier = modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(12.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          contentPadding = padding,
        ) {
          items(state.recipes, key = { it.id }, contentType = { "recipe_item" }) {
            RecipeCard(
              it,
              showAreaLabel = state.showAreaLabel,
              horizontalCards = horizontalCards,
              onClick = { state.eventSink(RecipesEvent.Success.RecipeClicked(it.id)) },
              Modifier.animateItem(),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun RecipeCard(
  recipe: Recipe,
  showAreaLabel: Boolean,
  horizontalCards: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  OutlinedCard(onClick = onClick, modifier = modifier.pointerHoverIcon(PointerIcon.Hand, true)) {
    if (horizontalCards) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        RecipeCardContent(
          // A definite size, not fillMaxHeight: a grid item is measured with an unbounded height,
          // so filling it is a no-op that leaves the image sitting at its minimum.
          recipeImage = { RecipeImage(recipe, modifier = Modifier.size(110.dp)) }
        ) {
          RecipeDetails(recipe, showAreaLabel, Modifier.padding(vertical = 8.dp))
        }
      }
    } else {
      Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 8.dp),
      ) {
        RecipeCardContent(
          recipeImage = { RecipeImage(recipe, modifier = Modifier.fillMaxWidth().aspectRatio(1f)) }
        ) {
          RecipeDetails(recipe, showAreaLabel, Modifier)
        }
      }
    }
  }
}

@Composable
private fun RecipeCardContent(
  recipeImage: @Composable () -> Unit,
  recipeDetails: @Composable () -> Unit,
) {
  recipeImage()
  recipeDetails()
}

@Composable
private fun RecipeDetails(recipe: Recipe, showAreaLabel: Boolean, modifier: Modifier = Modifier) {
  Column(
    verticalArrangement = Arrangement.spacedBy(4.dp),
    modifier = modifier.padding(horizontal = 8.dp),
  ) {
    Text(
      recipe.name,
      style = MaterialTheme.typography.labelMedium,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    val area = recipe.area
    if (showAreaLabel && area != null) {
      Text(area, style = MaterialTheme.typography.bodySmall)
    }
    val category = recipe.category
    if (category != null) {
      Text(category, style = MaterialTheme.typography.bodySmall)
    }
  }
}

@Composable
private fun RecipeImage(recipe: Recipe, modifier: Modifier = Modifier) {
  val thumbnail = recipe.thumbnail
  Box(modifier = modifier, contentAlignment = Alignment.BottomEnd) {
    AsyncImage(
      thumbnail,
      contentDescription = null,
      SingletonImageLoader.get(LocalPlatformContext.current),
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize(),
    )
    FavoriteIcon(recipe.favorite, modifier = Modifier.padding(8.dp).size(32.dp))
  }
}
