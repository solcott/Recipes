package com.scottolcott.recipe.ui.recipe

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalFlexBoxApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass.Companion.HEIGHT_DP_MEDIUM_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import com.scottolcott.recipe.LocalWindowSizeClass
import com.scottolcott.recipe.domain.presenter.RecipeDetailsEvent
import com.scottolcott.recipe.domain.presenter.RecipeDetailsScreen
import com.scottolcott.recipe.domain.presenter.RecipeDetailsState
import com.scottolcott.recipe.model.Recipe
import com.scottolcott.recipe.ui.Res
import com.scottolcott.recipe.ui.an_error_occurred
import com.scottolcott.recipe.ui.favorite_24px
import com.scottolcott.recipe.ui.favorite_24px_filled
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.zacsweers.metro.AppScope
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
      recipe?.let { RecipeDetails(it, state) }
    }
  }
}

@OptIn(ExperimentalFlexBoxApi::class)
@Composable
private fun RecipeDetails(recipe: Recipe, state: RecipeDetailsState) {
  val windowSizeClass = LocalWindowSizeClass.current

  val isExpandedWidth = windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_EXPANDED_LOWER_BOUND)
  val isShortHeight = !windowSizeClass.isHeightAtLeastBreakpoint(HEIGHT_DP_MEDIUM_LOWER_BOUND)

  val useSideBySide = isExpandedWidth || isShortHeight

  if (!useSideBySide) {
    // Standard Vertical Layout (Phone Portrait / Small Tablets)
    LazyColumn(
      Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item {
        RecipeImage(
          recipe,
          recipe.favorite,
          onToggleFavorite = { state.eventSink(RecipeDetailsEvent.ToggleFavorite) },
          modifier = Modifier.fillMaxWidth(),
        )
      }
      item { RecipeTextDetails(recipe) }
    }
  } else {
    // Side-by-Side Layout (Desktop / Tablets / Phone Landscape)
    Row(
      Modifier.fillMaxSize().padding(horizontal = if (isExpandedWidth) 32.dp else 16.dp),
      horizontalArrangement = Arrangement.spacedBy(if (isExpandedWidth) 32.dp else 16.dp),
    ) {
      Box(
        modifier =
          Modifier.weight(if (isExpandedWidth) 0.4f else 0.5f)
            .fillMaxHeight()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.TopCenter,
      ) {
        RecipeImage(
          recipe,
          recipe.favorite,
          onToggleFavorite = { state.eventSink(RecipeDetailsEvent.ToggleFavorite) },
          modifier = Modifier.fillMaxWidth(),
        )
      }

      LazyColumn(
        modifier = Modifier.weight(if (isExpandedWidth) 0.6f else 0.5f).fillMaxHeight(),
        contentPadding = PaddingValues(vertical = 16.dp),
      ) {
        item { RecipeTextDetails(recipe) }
      }
    }
  }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun RecipeTextDetails(recipe: Recipe, modifier: Modifier = Modifier) {
  SelectionContainer {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(recipe.name, style = MaterialTheme.typography.headlineMediumEmphasized)
      val details = recipe.details
      val ingredients = details?.ingredients.orEmpty()
      ingredients.forEach {
        Row(
          modifier = Modifier.padding(start = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text("•", style = MaterialTheme.typography.bodyMedium)
          Text("${it.measure} ${it.ingredient}", style = MaterialTheme.typography.bodyMedium)
        }
      }
      val instructions = details?.instructions
      instructions?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }
  }
}

@Composable
private fun RecipeImage(
  recipe: Recipe,
  isFavorite: Boolean,
  onToggleFavorite: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.aspectRatio(1f), contentAlignment = Alignment.BottomEnd) {
    AsyncImage(
      recipe.thumbnail,
      contentDescription = null,
      SingletonImageLoader.get(LocalPlatformContext.current),
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize(),
    )
    FavoriteIcon(
      isFavorite,
      modifier = Modifier.padding(12.dp).size(48.dp),
      onClick = onToggleFavorite,
    )
  }
}

@Composable
fun FavoriteIcon(
  isFavorite: Boolean,
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
) {
  Icon(
    if (isFavorite) painterResource(Res.drawable.favorite_24px_filled)
    else painterResource(Res.drawable.favorite_24px),
    contentDescription = "favorite",
    modifier =
      modifier.clickable(enabled = onClick != null) { onClick?.invoke() }.clip(CircleShape),
    tint = MaterialTheme.colorScheme.primary,
  )
}
