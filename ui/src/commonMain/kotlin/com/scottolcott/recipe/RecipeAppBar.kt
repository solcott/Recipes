package com.scottolcott.recipe

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.AppBarWithSearchColors
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExpandedDockedSearchBar
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import com.scottolcott.recipe.domain.presenter.RecipeScaffoldEvent
import com.scottolcott.recipe.domain.presenter.RecipeScaffoldState
import com.scottolcott.recipe.domain.presenter.RecipesScreen
import com.scottolcott.recipe.domain.presenter.SearchEvent
import com.scottolcott.recipe.model.SearchSuggestion
import com.scottolcott.recipe.ui.Res
import com.scottolcott.recipe.ui.arrow_back_24px
import com.scottolcott.recipe.ui.arrow_back_ios_24px
import com.scottolcott.recipe.ui.check_24px
import com.scottolcott.recipe.ui.chef_hat_24px
import com.scottolcott.recipe.ui.favorite_24px_filled
import com.scottolcott.recipe.ui.history_24px
import com.scottolcott.recipe.ui.recipes
import com.scottolcott.recipe.ui.search
import com.scottolcott.recipe.ui.search_24px
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.navigation.NavStack
import com.slack.circuit.runtime.navigation.canGoBack
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeAppBar(state: RecipeScaffoldState, modifier: Modifier = Modifier) {
  val scope = rememberCoroutineScope()
  val searchBarState = rememberSearchBarState(initialValue = SearchBarValue.Collapsed)
  val appBarWithSearchColors = getAppBarWithSearchColors()
  val keyboardController = LocalSoftwareKeyboardController.current

  val onSearch: (SearchSuggestion) -> Unit =
    remember(state.searchState, keyboardController, searchBarState, scope) {
      { query: SearchSuggestion ->
        scope.launch {
          when (query) {
            is SearchSuggestion.CategorySuggestion -> TODO()
            is SearchSuggestion.IngredientSuggestion -> TODO()
            is SearchSuggestion.QuerySuggestion ->
              state.searchState.eventSink(SearchEvent.PerformSearch(query.query))
          }
          keyboardController?.hide()
          searchBarState.animateToCollapsed()
        }
      }
    }

  val inputField =
    @Composable {
      RecipeSearchBarInputField(
        searchText = state.searchState.searchText,
        searchBarState = searchBarState,
        onSearch = { onSearch(SearchSuggestion.QuerySuggestion(it)) },
        colors = appBarWithSearchColors,
      )
    }

  AnimatedContent(state.searchState.isSearchActive) { isActive ->
    if (isActive) {
      AppBarWithSearch(
        searchBarState,
        inputField,
        colors = appBarWithSearchColors,
        modifier = modifier,
      )
      ExpandedSearchBar(searchBarState, inputField, appBarWithSearchColors, state, onSearch)
    } else {
      RecipeTopAppBar(state, modifier)
    }
  }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun RecipeSearchBarInputField(
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
@OptIn(ExperimentalMaterial3Api::class)
private fun RecipeTopAppBar(state: RecipeScaffoldState, modifier: Modifier = Modifier) {
  val title = @Composable { Text(stringResource(Res.string.recipes)) }
  val navigationIcon = @Composable { NavIcon(state.navStack, state.navigator) }
  val actions: @Composable RowScope.() -> Unit = {
    IconButton(
      onClick = { state.eventSink(RecipeScaffoldEvent.GoTo(RecipesScreen.Favorites)) },
      modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
    ) {
      Icon(painter = painterResource(Res.drawable.favorite_24px_filled), contentDescription = null)
    }
    IconButton(
      onClick = { state.searchState.eventSink(SearchEvent.SearchButtonClicked) },
      modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
    ) {
      Icon(painter = painterResource(Res.drawable.search_24px), contentDescription = null)
    }
  }

  if (isIos()) {
    CenterAlignedTopAppBar(
      title = title,
      navigationIcon = navigationIcon,
      actions = actions,
      modifier = modifier,
    )
  } else {
    TopAppBar(
      title = title,
      navigationIcon = navigationIcon,
      actions = actions,
      modifier = modifier,
    )
  }
}

@Composable
private fun NavIcon(backStack: NavStack<out NavStack.Record>, navigator: Navigator) {
  if (backStack.canGoBack) {
    IconButton(onClick = { navigator.pop() }) {
      val icon = if (isIos()) Res.drawable.arrow_back_ios_24px else Res.drawable.arrow_back_24px
      Icon(painter = painterResource(icon), contentDescription = null)
    }
  } else {
    IconButton(
      {},
      enabled = false,
      colors =
        IconButtonDefaults.iconButtonColors(disabledContentColor = LocalContentColor.current),
    ) {
      Icon(painter = painterResource(Res.drawable.chef_hat_24px), contentDescription = null)
    }
  }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ExpandedSearchBar(
  searchBarState: SearchBarState,
  inputField: @Composable () -> Unit,
  appBarWithSearchColors: AppBarWithSearchColors,
  state: RecipeScaffoldState,
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

private const val LEADING_IMAGE_ASPECT_RATIO = 233f / 145f

@OptIn(ExperimentalFoundationStyleApi::class)
@Composable
private fun SearchSuggestionItems(
  state: RecipeScaffoldState,
  onSearch: (SearchSuggestion) -> Unit,
) {

  LazyColumn(modifier = Modifier.fillMaxSize()) {
    items(
      state.searchState.suggestions.history,
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
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary),
          )
        },
        headlineContent = { Text(text, color = MaterialTheme.colorScheme.onPrimary) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier =
          Modifier.animateItem()
            .clickable {
              state.searchState.searchText.setTextAndPlaceCursorAtEnd(text)
              onSearch(it)
            }
            .fillMaxWidth(),
      )
    }
    items(
      state.searchState.suggestions.categories.categories,
      key = { "category_${it.id}" },
      contentType = { "category_or_ingredient_item" },
    ) {
      SuggestedItem(it.name, it.thumb, Modifier.animateItem().fillMaxWidth())
    }

    items(
      state.searchState.suggestions.ingredientSuggestions.ingredients,
      key = { "ingredient_${it.id}" },
      contentType = { "category_or_ingredient_item" },
    ) {
      SuggestedItem(it.name, "${it.thumbnail}/small", Modifier.animateItem())
    }
  }
}

@Composable
fun LazyItemScope.SuggestedItem(text: String, thumbnail: String, modifier: Modifier = Modifier) {
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
    modifier =
      modifier
        .animateItem()
        .clickable {
          //              onSearch(it)
        }
        .fillMaxWidth(),
  )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun getAppBarWithSearchColors(): AppBarWithSearchColors =
  SearchBarDefaults.appBarWithSearchColors(
    searchBarColors =
      SearchBarDefaults.colors(
        containerColor = MaterialTheme.colorScheme.primary,
        inputFieldColors =
          TextFieldDefaults.colors(
            cursorColor = MaterialTheme.colorScheme.onPrimary,
            focusedIndicatorColor = MaterialTheme.colorScheme.onPrimary,
            focusedContainerColor = MaterialTheme.colorScheme.primary,
            errorContainerColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.primary,
            unfocusedContainerColor = MaterialTheme.colorScheme.primary,
            focusedTextColor = MaterialTheme.colorScheme.onPrimary,
            unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
            disabledTextColor = MaterialTheme.colorScheme.onPrimary,
            focusedLabelColor = MaterialTheme.colorScheme.onPrimary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onPrimary,
            disabledLabelColor = MaterialTheme.colorScheme.onPrimary,
            focusedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
            disabledLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
            focusedTrailingIconColor = MaterialTheme.colorScheme.onPrimary,
            //            unfocusedTrailingIconColor = Color.Transparent,
            disabledTrailingIconColor = MaterialTheme.colorScheme.onPrimary.copy(0.47f),
            focusedPlaceholderColor = MaterialTheme.colorScheme.onPrimary,
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onPrimary,
            disabledPlaceholderColor = MaterialTheme.colorScheme.onPrimary.copy(0.47f),
          ),
      )
  )
