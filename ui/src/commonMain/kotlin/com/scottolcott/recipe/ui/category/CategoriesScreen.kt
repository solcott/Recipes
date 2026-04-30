@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.scottolcott.recipe.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import com.scottolcott.recipe.domain.presenter.CategoriesEvent
import com.scottolcott.recipe.domain.presenter.CategoriesScreen
import com.scottolcott.recipe.domain.presenter.CategoriesState
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import com.scottolcott.recipe.model.Category
import com.scottolcott.recipe.ui.Res
import com.scottolcott.recipe.ui.no_categories_found
import com.scottolcott.recipe.LocalWindowSizeClass
import com.scottolcott.recipe.ui.ErrorDisplay
import com.scottolcott.recipe.ui.rememberAdaptiveGridCells
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.zacsweers.metro.AppScope
import org.jetbrains.compose.resources.stringResource

@Suppress("unused")
@Composable
@CircuitInject(CategoriesScreen::class, AppScope::class)
fun CategoriesScreen(state: CategoriesState, modifier: Modifier = Modifier) {
  val cells = rememberAdaptiveGridCells()
  val labelTextStyle =
    if (LocalWindowSizeClass.current.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)) {
      MaterialTheme.typography.titleLargeEmphasized
    } else {
      MaterialTheme.typography.titleMediumEmphasized
    }
  when (state) {
    is CategoriesState.Error ->
      Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ErrorDisplay(onRetryClick = { state.eventSink(CategoriesEvent.Error.RetryClicked) })
      }
    CategoriesState.Loading ->
      Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
      }
    is CategoriesState.Success -> {
      if (state.categories.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text(stringResource(Res.string.no_categories_found))
        }
      } else {
        LazyVerticalGrid(
          cells,
          modifier = modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(12.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          contentPadding = PaddingValues(16.dp),
        ) {
          items(state.categories, key = { it.id }, contentType = { "category_item" }) {
            CategoryItem(
              it,
              Modifier.animateItem(),
              labelTextStyle = labelTextStyle,
              onCategoryClick = {
                state.eventSink(CategoriesEvent.Success.CategoryClicked(it.name))
              },
            )
          }
        }
      }
    }
  }
}

@Composable
fun CategoryItem(
  category: Category,
  modifier: Modifier = Modifier,
  labelTextStyle: TextStyle,
  onCategoryClick: () -> Unit,
) {

  OutlinedCard(modifier.clickable(true, onClick = onCategoryClick)) {
    AsyncImage(
      category.thumb,
      contentDescription = category.name,
      imageLoader = SingletonImageLoader.get(LocalPlatformContext.current),
      contentScale = ContentScale.Crop,
      modifier =
        Modifier.fillMaxWidth()
          .aspectRatio(320f / 200f)
          .background(MaterialTheme.colorScheme.surface),
    )
    Text(
      category.name,
      Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      style = labelTextStyle,
    )
  }
}
