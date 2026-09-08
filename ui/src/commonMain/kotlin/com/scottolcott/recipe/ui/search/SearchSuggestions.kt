@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.scottolcott.recipe.ui.search

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import com.scottolcott.recipe.domain.isCupertino
import com.scottolcott.recipe.domain.presenter.SearchState
import com.scottolcott.recipe.model.SearchSuggestion
import com.scottolcott.recipe.ui.LocalFloatingBarInset
import com.scottolcott.recipe.ui.Res
import com.scottolcott.recipe.ui.categories
import com.scottolcott.recipe.ui.delete_24px
import com.scottolcott.recipe.ui.history_24px
import com.scottolcott.recipe.ui.ingredients
import com.scottolcott.recipe.ui.recent
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val LEADING_IMAGE_ASPECT_RATIO = 233f / 145f
private val LeadingImageWidth = 64.dp

/**
 * Where a row's text begins: the leading image plus the list item's own 16dp margins.
 *
 * iOS insets a row separator to the text rather than running it to the edge, and the section header
 * of a *grouped* list aligns with the section's margin instead -- so this is the one measurement
 * both of those are stated against.
 */
private val RowTextInset = 96.dp

@Composable
internal fun SearchSuggestionItems(
  state: SearchState,
  onSearch: (SearchSuggestion) -> Unit,
  onRemoveSuggestionClick: (SearchSuggestion) -> Unit,
) {
  val listState = rememberLazyListState()
  // The only scrolling list here that does not pad itself with `rememberAdaptivePadding` -- a
  // suggestion row runs edge to edge, so it has no horizontal padding to inherit. It still has to
  // clear the floating tab bar, hence the bottom inset on its own.
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    state = listState,
    contentPadding = PaddingValues(bottom = LocalFloatingBarInset.current),
  ) {
    historySection(state, listState, onSearch, onRemoveSuggestionClick)
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
  onRemoveSuggestionClick: (SearchSuggestion) -> Unit,
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

    SuggestionRow(
      text = text,
      leadingContent = {
        Image(
          painter = painterResource(Res.drawable.history_24px),
          contentDescription = null,
          modifier = Modifier.width(LeadingImageWidth).aspectRatio(LEADING_IMAGE_ASPECT_RATIO),
          // The recents glyph is a secondary-label grey on iOS -- Safari, Maps and the App Store
          // all spend the tint on the row's action, never on its icon.
          colorFilter =
            ColorFilter.tint(
              if (isCupertino) {
                MaterialTheme.colorScheme.onSurfaceVariant
              } else {
                MaterialTheme.colorScheme.onPrimaryContainer
              }
            ),
          contentScale = ContentScale.Inside,
        )
      },
      onClick = {
        state.searchText.setTextAndPlaceCursorAtEnd(text)
        onSearch(it)
      },
      trailingContent = {
        IconButton(onClick = { onRemoveSuggestionClick(it) }) {
          Icon(
            painterResource(Res.drawable.delete_24px),
            "Delete",
            tint =
              if (isCupertino) {
                MaterialTheme.colorScheme.onSurfaceVariant
              } else {
                MaterialTheme.colorScheme.onPrimaryContainer
              },
          )
        }
      },
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
    )
  }
}

@Composable
private fun SectionHeader(headlineText: String, pinned: Boolean, modifier: Modifier = Modifier) {
  val cupertino = isCupertino
  // A pinned iOS list header does not lift off the page -- the flat grey band *is* the separation,
  // and a shadow under it is the tell that a Material list is wearing iOS colours.
  val elevation by
    animateDpAsState(if (pinned && !cupertino) 4.dp else 0.dp, label = "headerElevation")
  Surface(
    shadowElevation = elevation,
    // `background` is the grouped grey (see CupertinoColors); rows sit on `surface`, which is
    // lighter, so the header reads as the recessed band iOS draws between sections.
    color =
      if (cupertino) {
        MaterialTheme.colorScheme.background
      } else {
        MaterialTheme.colorScheme.onPrimary
          .copy(alpha = .1f)
          .compositeOver(MaterialTheme.colorScheme.primaryContainer)
      },
    modifier = modifier.fillMaxWidth(),
  ) {
    Text(
      headlineText.uppercase(),
      color =
        if (cupertino) {
          MaterialTheme.colorScheme.onSurfaceVariant
        } else {
          MaterialTheme.colorScheme.onPrimaryContainer
        },
      style =
        if (cupertino) {
          MaterialTheme.typography.bodySmallEmphasized
        } else {
          MaterialTheme.typography.titleSmall
        },
      // iOS aligns a section header to the page margin, not to the row text the way Material does.
      modifier =
        Modifier.padding(start = if (cupertino) 16.dp else RowTextInset, top = 8.dp, bottom = 8.dp)
          .fillMaxWidth(),
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
  SuggestionRow(
    text = text,
    leadingContent = {
      AsyncImage(
        thumbnail,
        contentDescription = "",
        modifier = Modifier.width(LeadingImageWidth).aspectRatio(LEADING_IMAGE_ASPECT_RATIO),
        imageLoader = SingletonImageLoader.get(LocalPlatformContext.current),
      )
    },
    onClick = onClick,
    modifier = modifier,
  )
}

/**
 * One suggestion row, in whichever design language is in force.
 *
 * The colour is the part that has to branch. Material's search sheet is the dark
 * `primaryContainer`, so its rows are written in `onPrimary`; the Cupertino sheet is
 * systemBackground, where that same white would be invisible -- rows there take the ordinary label
 * colour and are parted by an inset hairline instead.
 */
@Composable
private fun LazyItemScope.SuggestionRow(
  text: String,
  leadingContent: @Composable () -> Unit,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  trailingContent: @Composable (() -> Unit)? = null,
) {
  val cupertino = isCupertino
  Column(modifier.animateItem().fillMaxWidth()) {
    ListItem(
      leadingContent = leadingContent,
      headlineContent = {
        Text(
          text,
          color =
            if (cupertino) {
              MaterialTheme.colorScheme.onSurface
            } else {
              MaterialTheme.colorScheme.onPrimary
            },
        )
      },
      trailingContent = trailingContent,
      colors = ListItemDefaults.colors(containerColor = Color.Transparent),
      // No `overrideDescendants`: the trailing delete button declares no cursor of its own, so it
      // inherits this one -- which is the cursor it wants anyway. Overriding here would only take
      // that choice away from any future child that needs a different one.
      modifier =
        Modifier.clickable(onClick = onClick).pointerHoverIcon(PointerIcon.Hand).fillMaxWidth(),
    )
    if (cupertino) {
      HorizontalDivider(
        modifier = Modifier.padding(start = RowTextInset),
        thickness = Dp.Hairline,
        color = MaterialTheme.colorScheme.outlineVariant,
      )
    }
  }
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
