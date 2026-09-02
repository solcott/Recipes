package com.scottolcott.recipe.ui.search

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import com.scottolcott.recipe.domain.presenter.SearchState
import com.scottolcott.recipe.model.SearchSuggestion
import com.scottolcott.recipe.ui.Res
import com.scottolcott.recipe.ui.categories
import com.scottolcott.recipe.ui.history_24px
import com.scottolcott.recipe.ui.ingredients
import com.scottolcott.recipe.ui.recent
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val LEADING_IMAGE_ASPECT_RATIO = 233f / 145f

@Composable
internal fun SearchSuggestionItems(state: SearchState, onSearch: (SearchSuggestion) -> Unit) {
  val listState = rememberLazyListState()
  LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
    historySection(state, listState, onSearch)
    categorySection(state, listState, onSearch)
    ingredientSection(state, listState, onSearch)
  }
}

@OptIn(ExperimentalFoundationStyleApi::class)
private fun LazyListScope.sectionHeader(
  key: String,
  title: StringResource,
  listState: LazyListState,
) {
  stickyHeader(key = key, contentType = "section_header") { index ->
    SectionHeader(stringResource(title), rememberIsPinned(listState, index))
  }
}

private fun LazyListScope.historySection(
  state: SearchState,
  listState: LazyListState,
  onSearch: (SearchSuggestion) -> Unit,
) {
  if (state.suggestions.history.isEmpty()) return
  sectionHeader("recents_header", Res.string.recent, listState)
  items(
    state.suggestions.history,
    key = {
      when (it) {
        is SearchSuggestion.CategorySuggestion -> it.category.id
        is SearchSuggestion.IngredientSuggestion -> it.ingredient.id
        is SearchSuggestion.QuerySuggestion -> it.query
      }
    },
    contentType = { "history_item" },
  ) {
    val text =
      when (it) {
        is SearchSuggestion.CategorySuggestion -> it.category.name
        is SearchSuggestion.IngredientSuggestion -> it.ingredient.name
        is SearchSuggestion.QuerySuggestion -> it.query
      }

    ListItem(
      leadingContent = {
        Image(
          painter = painterResource(Res.drawable.history_24px),
          contentDescription = null,
          modifier = Modifier.width(64.dp).aspectRatio(LEADING_IMAGE_ASPECT_RATIO),
          colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer),
          contentScale = ContentScale.Inside,
        )
      },
      headlineContent = { Text(text, color = MaterialTheme.colorScheme.onPrimary) },
      colors = ListItemDefaults.colors(containerColor = Color.Transparent),
      modifier =
        Modifier.animateItem()
          .clickable {
            state.searchText.setTextAndPlaceCursorAtEnd(text)
            onSearch(it)
          }
          .fillMaxWidth(),
    )
  }
}

private fun LazyListScope.categorySection(
  state: SearchState,
  listState: LazyListState,
  onSearch: (SearchSuggestion) -> Unit,
) {
  val categories = state.suggestions.categories.categories
  if (categories.isEmpty()) return
  sectionHeader("categories_header", Res.string.categories, listState)
  items(
    categories,
    key = { "category_${it.id}" },
    contentType = { "category_or_ingredient_item" },
  ) {
    SuggestedItem(
      it.name,
      it.thumb,
      onClick = { onSearch(SearchSuggestion.CategorySuggestion(it)) },
      Modifier.animateItem().fillMaxWidth(),
    )
  }
}

private fun LazyListScope.ingredientSection(
  state: SearchState,
  listState: LazyListState,
  onSearch: (SearchSuggestion) -> Unit,
) {
  val ingredients = state.suggestions.ingredientSuggestions.ingredients
  if (ingredients.isEmpty()) return
  sectionHeader("ingredients_header", Res.string.ingredients, listState)
  items(
    ingredients,
    key = { "ingredient_${it.id}" },
    contentType = { "category_or_ingredient_item" },
  ) {
    SuggestedItem(
      it.name,
      "${it.thumbnail}/small",
      onClick = { onSearch(SearchSuggestion.IngredientSuggestion(it)) },
      Modifier.animateItem(),
    )
  }
}

@Composable
private fun SectionHeader(headlineText: String, pinned: Boolean, modifier: Modifier = Modifier) {
  val elevation by animateDpAsState(if (pinned) 4.dp else 0.dp, label = "headerElevation")
  Surface(
    shadowElevation = elevation,
    color =
      MaterialTheme.colorScheme.onPrimary
        .copy(alpha = .1f)
        .compositeOver(MaterialTheme.colorScheme.primaryContainer),
    modifier = modifier.fillMaxWidth(),
  ) {
    Text(
      headlineText.uppercase(),
      color = MaterialTheme.colorScheme.onPrimaryContainer,
      style = MaterialTheme.typography.titleSmall,
      modifier = Modifier.padding(start = 96.dp, top = 8.dp, bottom = 8.dp).fillMaxWidth(),
    )
  }
}

@Composable
private fun LazyItemScope.SuggestedItem(
  text: String,
  thumbnail: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  ListItem(
    leadingContent = {
      AsyncImage(
        thumbnail,
        contentDescription = "",
        modifier = Modifier.width(64.dp).aspectRatio(LEADING_IMAGE_ASPECT_RATIO),
        imageLoader = SingletonImageLoader.get(LocalPlatformContext.current),
      )
    },
    headlineContent = { Text(text, color = MaterialTheme.colorScheme.onPrimary) },
    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    modifier = modifier.animateItem().clickable(onClick = onClick).fillMaxWidth(),
  )
}

@Composable
private fun rememberIsPinned(state: LazyListState, headerIndex: Int): Boolean {
  val pinned by
    remember(state, headerIndex) {
      derivedStateOf {
        val info = state.layoutInfo
        val self = info.visibleItemsInfo.firstOrNull { it.index == headerIndex }
        // Present, parked at the viewport's leading edge, and not merely the
        // first item resting at scroll position zero.
        self != null &&
          self.offset <= info.viewportStartOffset &&
          (state.firstVisibleItemIndex > headerIndex ||
            (state.firstVisibleItemIndex == headerIndex && state.firstVisibleItemScrollOffset > 0))
      }
    }
  return pinned
}
