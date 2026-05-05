package com.scottolcott.recipe

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.AppBarWithSearchColors
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.window.core.layout.WindowSizeClass
import com.scottolcott.recipe.domain.presenter.RecipeScaffoldEvent
import com.scottolcott.recipe.domain.presenter.RecipeScaffoldState
import com.scottolcott.recipe.domain.presenter.RecipesScreen
import com.scottolcott.recipe.domain.presenter.SearchEvent
import com.scottolcott.recipe.ui.Res
import com.scottolcott.recipe.ui.arrow_back_24px
import com.scottolcott.recipe.ui.check_24px
import com.scottolcott.recipe.ui.chef_hat_24px
import com.scottolcott.recipe.ui.favorite_24px_filled
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

  val onSearch: (String) -> Unit =
    remember(state.searchState, keyboardController, searchBarState, scope) {
      { query: String ->
        scope.launch {
          state.searchState.eventSink(SearchEvent.PerformSearch(query))
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
        onSearch = onSearch,
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
  TopAppBar(
    title = { Text(stringResource(Res.string.recipes)) },
    navigationIcon = { NavIcon(state.navStack, state.navigator) },
    actions = {
      IconButton(onClick = { state.eventSink(RecipeScaffoldEvent.GoTo(RecipesScreen.Favorites)) }) {
        Icon(
          painter = painterResource(Res.drawable.favorite_24px_filled),
          contentDescription = null,
        )
      }
      IconButton(onClick = { state.searchState.eventSink(SearchEvent.SearchButtonClicked) }) {
        Icon(painter = painterResource(Res.drawable.search_24px), contentDescription = null)
      }
    },
    modifier = modifier,
  )
}

@Composable
private fun NavIcon(backStack: NavStack<out NavStack.Record>, navigator: Navigator) {
  if (backStack.canGoBack) {
    IconButton(onClick = { navigator.pop() }) {
      Icon(painter = painterResource(Res.drawable.arrow_back_24px), contentDescription = null)
    }
  } else {
    IconButton(
      {},
      enabled = false,
      colors = IconButtonDefaults.iconButtonColors(disabledContentColor = LocalContentColor.current),
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
  onSearch: (String) -> Unit,
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

@Composable
private fun SearchSuggestionItems(state: RecipeScaffoldState, onSearch: (String) -> Unit) {
  LazyColumn {
    items(state.searchState.suggestions) {
      ListItem(
        headlineContent = { Text(it, color = MaterialTheme.colorScheme.onPrimary) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier =
          Modifier.animateItem()
            .clickable {
              state.searchState.searchText.setTextAndPlaceCursorAtEnd(it)
              onSearch(it)
            }
            .fillMaxWidth(),
      )
    }
  }
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
