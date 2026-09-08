package com.scottolcott.recipe.ui.ingredient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import com.scottolcott.recipe.domain.LocalWindowSizeClass
import com.scottolcott.recipe.domain.presenter.IngredientsEvent
import com.scottolcott.recipe.domain.presenter.IngredientsScreen
import com.scottolcott.recipe.domain.presenter.IngredientsState
import com.scottolcott.recipe.model.Ingredient
import com.scottolcott.recipe.ui.ErrorDisplay
import com.scottolcott.recipe.ui.Res
import com.scottolcott.recipe.ui.design.AppCard
import com.scottolcott.recipe.ui.isShortWindow
import com.scottolcott.recipe.ui.no_ingredients_found
import com.scottolcott.recipe.ui.rememberAdaptiveGridCells
import com.scottolcott.recipe.ui.rememberAdaptivePadding
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.zacsweers.metro.AppScope
import org.jetbrains.compose.resources.stringResource

@Suppress("unused")
@Composable
@CircuitInject(IngredientsScreen::class, AppScope::class)
fun IngredientScreen(state: IngredientsState, modifier: Modifier = Modifier) {
  val cells = rememberAdaptiveGridCells(targetWidth = 165.dp, shortWindowTargetWidth = 200.dp)
  val padding = rememberAdaptivePadding()
  // A short window is starved of vertical space even when it is wide -- a landscape phone -- so the
  // larger label is held back for windows that are both wide and tall.
  val roomyLabel =
    LocalWindowSizeClass.current.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND) &&
      !isShortWindow()
  val labelTextStyle =
    if (roomyLabel) {
      MaterialTheme.typography.titleMediumEmphasized
    } else {
      MaterialTheme.typography.titleSmallEmphasized
    }
  Box(modifier, contentAlignment = Alignment.TopCenter) {
    when (state) {
      is IngredientsState.Error ->
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          ErrorDisplay(onRetryClick = { state.eventSink(IngredientsEvent.Error.RetryClicked) })
        }

      IngredientsState.Loading ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator()
        }

      is IngredientsState.Success -> {
        if (state.ingredients.isEmpty()) {
          Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(Res.string.no_ingredients_found))
          }
        } else {
          LazyVerticalGrid(
            cells,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = padding,
          ) {
            items(state.ingredients, key = { it.id }, contentType = { "ingredient_item" }) {
              IngredientItem(
                it,
                labelTextStyle = labelTextStyle,
                { state.eventSink(IngredientsEvent.Success.IngredientClicked(it.name)) },
                Modifier.animateItem().pointerHoverIcon(PointerIcon.Hand, true),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun IngredientItem(
  ingredient: Ingredient,
  labelTextStyle: TextStyle,
  onIngredientClick: () -> Unit,
  modifier: Modifier = Modifier,
) {

  AppCard(onClick = onIngredientClick, modifier = modifier) {
    AsyncImage(
      ingredient.thumbnail,
      contentDescription = ingredient.name,
      imageLoader = SingletonImageLoader.get(LocalPlatformContext.current),
      contentScale = ContentScale.Crop,
      modifier =
        Modifier.fillMaxWidth()
          .aspectRatio(320f / 200f)
          .background(MaterialTheme.colorScheme.surface),
    )
    Text(
      ingredient.name,
      Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      style = labelTextStyle,
      maxLines = 1,
    )
  }
}
