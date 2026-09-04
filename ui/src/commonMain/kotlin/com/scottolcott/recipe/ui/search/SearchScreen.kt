package com.scottolcott.recipe.ui.search

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.AppBarWithSearchColors
import androidx.compose.material3.ExpandedDockedSearchBar
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.window.core.layout.WindowSizeClass
import com.scottolcott.recipe.domain.LocalWindowSizeClass
import com.scottolcott.recipe.domain.presenter.SearchEvent
import com.scottolcott.recipe.domain.presenter.SearchScreen
import com.scottolcott.recipe.domain.presenter.SearchState
import com.scottolcott.recipe.model.SearchSuggestion
import com.scottolcott.recipe.ui.Res
import com.scottolcott.recipe.ui.check_24px
import com.scottolcott.recipe.ui.search
import com.scottolcott.recipe.ui.search_24px
import com.slack.circuit.subcircuit.SubCircuitInject
import com.slack.circuit.subcircuit.SubUi
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(state: SearchState, modifier: Modifier = Modifier) {
  val scope = rememberCoroutineScope()

  val appBarWithSearchColors = getAppBarWithSearchColors()
  val keyboardController = LocalSoftwareKeyboardController.current
  val onSearch: (SearchSuggestion) -> Unit =
    remember(state, keyboardController, state.searchBarState, scope) {
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
          state.searchBarState.animateToCollapsed()
        }
      }
    }
  val inputField =
    @Composable {
      RecipeSearchBarInputField(
        searchText = state.searchText,
        searchBarState = state.searchBarState,
        onSearch = { onSearch(SearchSuggestion.QuerySuggestion(it)) },
        colors = appBarWithSearchColors,
      )
    }
  AppBarWithSearch(
    state.searchBarState,
    inputField,
    colors = appBarWithSearchColors,
    modifier = modifier,
  )
  ExpandedSearchBar(
    state.searchBarState,
    inputField,
    appBarWithSearchColors,
    state,
    onSearch,
    { state.eventSink(SearchEvent.RemoveSearchSuggestion(it)) },
  )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ExpandedSearchBar(
  searchBarState: SearchBarState,
  inputField: @Composable () -> Unit,
  appBarWithSearchColors: AppBarWithSearchColors,
  state: SearchState,
  onSearch: (SearchSuggestion) -> Unit,
  onRemoveSuggestionClick: (SearchSuggestion) -> Unit,
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
      properties = dockedSearchBarPopupProperties(),
    ) {
      SearchSuggestionItems(state, onSearch, onRemoveSuggestionClick)
    }
  } else {
    ExpandedFullScreenSearchBar(
      state = searchBarState,
      inputField = inputField,
      modifier = Modifier.boundedToWindow(),
      colors = appBarWithSearchColors.searchBarColors,
    ) {
      SearchSuggestionItems(state, onSearch, onRemoveSuggestionClick)
    }
  }
}

/**
 * Replaces unbounded measure constraints with the window size.
 *
 * On iOS the expanded search bar lives in a `ComposeSceneLayer`, and the layer measures its content
 * with infinite constraints whenever its `ComposeScene.size` is null. Material3's
 * `FullScreenSearchBarLayout` feeds `constraints.maxWidth`/`maxHeight` straight into
 * `Constraints.fixed(...)`, which throws on infinity. Rotating the device is one way to get the
 * layer into that state, because `ComposeSceneMediator.prepareAndGetSizeTransitionAnimation`
 * latches its "layout transition animating" flag before its early return and then never clears it,
 * permanently suppressing the update that would set the scene size.
 *
 * Every other platform measures the dialog with bounded constraints already, so this is a
 * pass-through there.
 */
@Composable
private fun Modifier.boundedToWindow(): Modifier {
  val containerSize = LocalWindowInfo.current.containerSize
  return layout { measurable, constraints ->
    val placeable =
      measurable.measure(
        constraints.copy(
          maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else containerSize.width,
          maxHeight =
            if (constraints.hasBoundedHeight) constraints.maxHeight else containerSize.height,
        )
      )
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
  }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun getAppBarWithSearchColors(): AppBarWithSearchColors {
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

/**
 * Registers [SearchScreen] as the sub-circuit's UI.
 *
 * Metro also accepts `@SubCircuitInject` straight on a top-level composable, which would make this
 * class unnecessary — but the `SubUiFactory` it generates for a *function* holds a reference to
 * that function, and lowering that reference crashes the Kotlin/JS and Kotlin/Wasm back-ends
 * (`UpgradeCallableReferences`, IndexOutOfBounds). Only the two web targets are affected; JVM,
 * Android and native compile it happily, so a build that skipped them would look fine.
 *
 * A class target is generated without the reference, and it costs nothing: [SearchScreen] stays a
 * plain composable with the default `modifier` the project's conventions ask for, and stays
 * previewable, which an override of `Content` would not be.
 */
@SubCircuitInject(SearchScreen::class, AppScope::class)
@Inject
class SearchScreenSubUi : SubUi<SearchState> {
  @Composable
  override fun Content(state: SearchState, modifier: Modifier) {
    SearchScreen(state, modifier)
  }
}
