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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.AppBarWithSearchColors
import androidx.compose.material3.ExpandedDockedSearchBar
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import com.scottolcott.recipe.LocalWindowSizeClass
import com.scottolcott.recipe.domain.presenter.SearchEvent
import com.scottolcott.recipe.domain.presenter.SearchScreen
import com.scottolcott.recipe.domain.presenter.SearchState
import com.scottolcott.recipe.model.SearchSuggestion
import com.scottolcott.recipe.ui.Res
import com.scottolcott.recipe.ui.categories
import com.scottolcott.recipe.ui.check_24px
import com.scottolcott.recipe.ui.history_24px
import com.scottolcott.recipe.ui.ingredients
import com.scottolcott.recipe.ui.recent
import com.scottolcott.recipe.ui.search
import com.scottolcott.recipe.ui.search_24px
import com.slack.circuit.subcircuit.SubCircuitInject
import dev.zacsweers.metro.AppScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val LEADING_IMAGE_ASPECT_RATIO = 233f / 145f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@SubCircuitInject(SearchScreen::class, AppScope::class)
fun SearchScreen(state: SearchState, modifier: Modifier = Modifier) {
  val scope = rememberCoroutineScope()
  val searchBarState = rememberSearchBarState(initialValue = SearchBarValue.Collapsed)
  val appBarWithSearchColors = getAppBarWithSearchColors()
  val keyboardController = LocalSoftwareKeyboardController.current
  val onSearch: (SearchSuggestion) -> Unit =
    remember(state, keyboardController, searchBarState, scope) {
      { suggestion: SearchSuggestion ->
        scope.launch {
          when (suggestion) {
            is SearchSuggestion.CategorySuggestion ->
              state.eventSink(SearchEvent.CategoryItemClicked(suggestion.category))
            is SearchSuggestion.IngredientSuggestion ->
              state.eventSink(SearchEvent.IngredientItemClicked(suggestion.ingredient))
            is SearchSuggestion.QuerySuggestion ->
              state.eventSink(SearchEvent.PerformSearch(suggestion.query))
          }
          keyboardController?.hide()
          searchBarState.animateToCollapsed()
        }
      }
    }

  val inputField =
    @Composable {
      RecipeSearchBarInputField(
        searchText = state.searchText,
        searchBarState = searchBarState,
        onSearch = { onSearch(SearchSuggestion.QuerySuggestion(it)) },
        colors = appBarWithSearchColors,
      )
    }
  AppBarWithSearch(
    searchBarState,
    inputField,
    colors = appBarWithSearchColors,
    modifier = modifier,
  )
  ExpandedSearchBar(searchBarState, inputField, appBarWithSearchColors, state, onSearch)
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ExpandedSearchBar(
  searchBarState: SearchBarState,
  inputField: @Composable () -> Unit,
  appBarWithSearchColors: AppBarWithSearchColors,
  state: SearchState,
  onSearch: (SearchSuggestion) -> Unit,
) {
  val windowSizeClass = LocalWindowSizeClass.current
  if (
    windowSizeClass.isAtLeastBreakpoint(
      WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND,
      WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND,
    )
  ) {
    ExpandedDockedSearchBar(
      state = searchBarState,
      inputField = inputField,
      colors = appBarWithSearchColors.searchBarColors,
    ) {
      SearchSuggestionItems(state, onSearch)
    }
  } else {
    ExpandedFullScreenSearchBar(
      state = searchBarState,
      inputField = inputField,
      colors = appBarWithSearchColors.searchBarColors,
    ) {
      SearchSuggestionItems(state, onSearch)
    }
  }
}

@OptIn(ExperimentalFoundationStyleApi::class)
@Composable
private fun SearchSuggestionItems(
  state: SearchState,
  onSearch: (SearchSuggestion) -> Unit,
) {

  val listState = rememberLazyListState()
  LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
    if (state.suggestions.history.isNotEmpty()) {
      stickyHeader(key = "recents_header", contentType = "section_header") { index ->
        SectionHeader(stringResource(Res.string.recent), rememberIsPinned(listState, index))
      }
    }
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
    if (state.suggestions.categories.categories.isNotEmpty()) {

      stickyHeader(key = "categories_header", contentType = "section_header") { index ->
        SectionHeader(
          stringResource(Res.string.categories),
          rememberIsPinned(listState, index),
        )
      }
    }
    items(
      state.suggestions.categories.categories,
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

    if (state.suggestions.ingredientSuggestions.ingredients.isNotEmpty()) {
      stickyHeader(key = "ingredients_header", contentType = "section_header") { index ->
        SectionHeader(
          stringResource(Res.string.ingredients),
          pinned = rememberIsPinned(listState, index),
        )
      }
    }

    items(
      state.suggestions.ingredientSuggestions.ingredients,
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
fun LazyItemScope.SuggestedItem(
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
@OptIn(ExperimentalMaterial3Api::class)
fun getAppBarWithSearchColors(): AppBarWithSearchColors {
  val onPrimaryContainer = MaterialTheme.colorScheme.onPrimary
  val disabledTextColor = onPrimaryContainer.copy(0.47f)
  val primaryContainer = MaterialTheme.colorScheme.primaryContainer
  return SearchBarDefaults.appBarWithSearchColors(
    searchBarColors =
      SearchBarDefaults.colors(
        containerColor = primaryContainer,
        inputFieldColors =
          TextFieldDefaults.colors(
            cursorColor = onPrimaryContainer,
            focusedIndicatorColor = onPrimaryContainer,
            focusedContainerColor = primaryContainer,
            errorContainerColor = primaryContainer,
            disabledContainerColor = primaryContainer.copy(0.47f),
            unfocusedContainerColor = primaryContainer,
            focusedTextColor = onPrimaryContainer,
            unfocusedTextColor = onPrimaryContainer,
            disabledTextColor = disabledTextColor,
            focusedLabelColor = onPrimaryContainer,
            unfocusedLabelColor = onPrimaryContainer,
            disabledLabelColor = disabledTextColor,
            focusedLeadingIconColor = onPrimaryContainer,
            unfocusedLeadingIconColor = onPrimaryContainer,
            disabledLeadingIconColor = disabledTextColor,
            focusedTrailingIconColor = onPrimaryContainer,
            //            unfocusedTrailingIconColor = Color.Transparent,
            disabledTrailingIconColor = disabledTextColor,
            focusedPlaceholderColor = onPrimaryContainer,
            unfocusedPlaceholderColor = onPrimaryContainer,
            disabledPlaceholderColor = disabledTextColor,
          ),
      )
  )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun RecipeSearchBarInputField(
  searchText: TextFieldState,
  searchBarState: SearchBarState,
  onSearch: (String) -> Unit,
  colors: AppBarWithSearchColors,
  modifier: Modifier = Modifier,
) {
  SearchBarDefaults.InputField(
    modifier = modifier.fillMaxWidth(),
    textFieldState = searchText,
    searchBarState = searchBarState,
    colors = colors.searchBarColors.inputFieldColors,
    onSearch = onSearch,
    placeholder = {
      Text(modifier = Modifier.clearAndSetSemantics {}, text = stringResource(Res.string.search))
    },
    leadingIcon = {
      Icon(painter = painterResource(Res.drawable.search_24px), contentDescription = "")
    },
    trailingIcon = {
      if (searchBarState.currentValue == SearchBarValue.Expanded) {
        IconButton(
          { onSearch(searchText.text.toString()) },
          enabled = searchText.text.length >= 3,
        ) {
          Icon(painter = painterResource(Res.drawable.check_24px), contentDescription = "")
        }
      }
    },
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
