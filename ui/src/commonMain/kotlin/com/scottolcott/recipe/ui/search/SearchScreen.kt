package com.scottolcott.recipe.ui.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.AppBarWithSearchColors
import androidx.compose.material3.ExpandedDockedSearchBar
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarColors
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.scottolcott.recipe.LocalAppBarNavigationIcon
import com.scottolcott.recipe.domain.LocalWindowSizeClass
import com.scottolcott.recipe.domain.isCupertino
import com.scottolcott.recipe.domain.presenter.SearchEvent
import com.scottolcott.recipe.domain.presenter.SearchScreen
import com.scottolcott.recipe.domain.presenter.SearchState
import com.scottolcott.recipe.isEscapeShortcut
import com.scottolcott.recipe.model.SearchSuggestion
import com.scottolcott.recipe.ui.Res
import com.scottolcott.recipe.ui.cancel
import com.scottolcott.recipe.ui.cancel_24px
import com.scottolcott.recipe.ui.check_24px
import com.scottolcott.recipe.ui.clear_search
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
  val collapse: () -> Unit = {
    scope.launch {
      keyboardController?.hide()
      state.searchBarState.animateToCollapsed()
    }
  }
  val inputField =
    @Composable {
      SearchInputField(
        state = state,
        colors = appBarWithSearchColors,
        onSearch = { onSearch(SearchSuggestion.QuerySuggestion(it)) },
        onCancel = collapse,
      )
    }
  AppBarWithSearch(
    state.searchBarState,
    inputField,
    // On a layout wide enough to keep the search bar up permanently this bar *is* the top app bar,
    // so the back control travels with it. Supplied by `RecipeAppBar`; null when this screen is
    // used on its own, and on web, where the browser's back button makes it redundant.
    navigationIcon = LocalAppBarNavigationIcon.current,
    colors = appBarWithSearchColors,
    modifier = modifier.padding(horizontal = 16.dp),
  )
  ExpandedSearchBar(
    state.searchBarState,
    inputField,
    appBarWithSearchColors.searchBarColors,
    state,
    onSearch,
    { state.eventSink(SearchEvent.RemoveSearchSuggestion(it)) },
  )
}

/**
 * How much has to be typed before the field will submit.
 *
 * Named so the enabled state and the cursor that advertises it are stated once rather than twice --
 * see the trailing icon in [RecipeSearchBarInputField].
 */
private const val MIN_SEARCH_LENGTH = 3

/**
 * The search field, plus whatever the design puts beside it.
 *
 * The same instance backs both the collapsed app bar and the expanded overlay -- Material morphs
 * one into the other -- so anything that should appear only while searching is conditioned on the
 * bar's own state rather than on which of the two is calling.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SearchInputField(
  state: SearchState,
  colors: AppBarWithSearchColors,
  onSearch: (String) -> Unit,
  onCancel: () -> Unit,
) {
  val expanded = state.searchBarState.currentValue == SearchBarValue.Expanded
  val cupertino = isCupertino
  val field =
    @Composable { fieldModifier: Modifier ->
      RecipeSearchBarInputField(
        searchText = state.searchText,
        searchBarState = state.searchBarState,
        onSearch = onSearch,
        colors = colors,
        // Escape is handled here rather than at the desktop window because the expanded bar is a
        // ComposeSceneLayer of its own: keys typed into it never reach the main window's handler.
        // The field holds focus while the bar is open, so a preview handler on it always sees them.
        onEscape = {
          if (expanded) onCancel()
          expanded
        },
        modifier = fieldModifier,
      )
    }
  if (!cupertino) {
    field(Modifier.fillMaxWidth())
    return
  }
  // The Cancel button beside the field is the single most recognisable part of an iOS search
  // screen -- it is how the screen is dismissed, and it is where the tint colour is spent. Outside
  // the field rather than in its trailing slot, because that is where iOS puts it and because the
  // field visibly shortens to make room for it.
  Row(
    verticalAlignment = Alignment.CenterVertically,
    // The full-screen bar insets its input field by 8dp where the app bar hosting the collapsed one
    // already pads 16; the expanded state makes up the difference so the field and the Cancel
    // beside it sit on iOS's margin rather than against the screen edge.
    modifier = Modifier.fillMaxWidth().padding(horizontal = if (expanded) 8.dp else 0.dp),
  ) {
    field(Modifier.weight(1f))
    if (expanded) {
      TextButton(
        onClick = onCancel,
        contentPadding = PaddingValues(start = 12.dp),
        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
      ) {
        Text(stringResource(Res.string.cancel), style = MaterialTheme.typography.bodyLarge)
      }
    }
  }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ExpandedSearchBar(
  searchBarState: SearchBarState,
  inputField: @Composable () -> Unit,
  colors: SearchBarColors,
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
      colors = colors,
      properties = dockedSearchBarPopupProperties(),
    ) {
      SuggestionsContent(state, onSearch, onRemoveSuggestionClick)
    }
  } else {
    ExpandedFullScreenSearchBar(
      state = searchBarState,
      inputField = inputField,
      modifier = Modifier.boundedToWindow(),
      colors = colors,
    ) {
      SuggestionsContent(state, onSearch, onRemoveSuggestionClick)
    }
  }
}

/**
 * The suggestion list, with a hairline of progress across its top while any source is still
 * answering.
 *
 * A hairline over the content rather than a spinner in place of it, because each source keeps
 * showing what it last had until its new answer lands. Drawn over the list rather than above it, so
 * a request starting or finishing never shifts the rows.
 */
@Composable
private fun SuggestionsContent(
  state: SearchState,
  onSearch: (SearchSuggestion) -> Unit,
  onRemoveSuggestionClick: (SearchSuggestion) -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier) {
    SearchSuggestionItems(state, onSearch, onRemoveSuggestionClick)
    if (state.suggestions.isAnyLoading) {
      LinearProgressIndicator(Modifier.fillMaxWidth())
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
  // iOS search fields are a neutral fill with ordinary label text -- the accent is spent on the
  // Cancel button, never on the field itself. Material's is the tinted container it has always
  // been here. Only the four colours named here differ; everything below is derived from them.
  val cupertino = isCupertino
  val scheme = MaterialTheme.colorScheme
  val onPrimaryContainer = if (cupertino) scheme.onSurface else scheme.onPrimary
  val disabledTextColor = onPrimaryContainer.copy(0.47f)
  val primaryContainer = if (cupertino) scheme.surfaceVariant else scheme.primaryContainer
  // The magnifier and the "Search" placeholder are secondary-label grey on iOS; full label contrast
  // is reserved for what the user has actually typed. Material draws both at the same weight.
  val hintColor = if (cupertino) scheme.onSurfaceVariant else onPrimaryContainer
  // iOS carets take the tint colour rather than the text colour.
  val cursorColor = if (cupertino) scheme.primary else onPrimaryContainer
  // What the suggestion list sits on once the bar is expanded. Under Cupertino that is
  // systemBackground -- `surface` here, see CupertinoColors -- because on iOS the grey belongs to
  // the field, not to the page behind the results. Left at the field's fill it produced a screen
  // that was one flat grey from the status bar down.
  val sheetContainer = if (cupertino) scheme.surface else primaryContainer
  return SearchBarDefaults.appBarWithSearchColors(
    // `AppBarWithSearch` paints its own Surface behind the field, and that colour defaults to the
    // `Surface` token -- which the Cupertino scheme maps to *white*, because there `surface` is
    // `secondarySystemGroupedBackground`, the raised row colour, while the page is `background`.
    // Left defaulted, it puts a white band around the field that matches nothing beside it, and
    // makes the app bar change colour depending on whether search happens to be showing.
    //
    // Transparent rather than `background`: it is the value Material3 documents for "no container"
    // and the one its own `TopSearchBar` passes, the `Scaffold` behind already paints `background`,
    // and it stays right if this bar is ever given a glass backdrop. Both elevations here are
    // Level0, so nothing casts a shadow in the transparent path.
    // The `else` arms restate Material3's own defaults (the AppBar tokens they read are internal),
    // so the Material design is left exactly as it was.
    appBarContainerColor = if (cupertino) Color.Transparent else scheme.surface,
    scrolledAppBarContainerColor = if (cupertino) Color.Transparent else scheme.surfaceContainer,
    // Inert today -- the back button tints itself and nothing occupies the actions slot -- but the
    // defaults are onSurface/onSurfaceVariant, which would be wrong for iOS the moment either does.
    appBarNavigationIconColor = if (cupertino) scheme.primary else scheme.onSurface,
    appBarActionIconColor = if (cupertino) scheme.primary else scheme.onSurfaceVariant,
    searchBarColors =
      SearchBarDefaults.colors(
        containerColor = sheetContainer,
        inputFieldColors =
          TextFieldDefaults.colors(
            cursorColor = cursorColor,
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
            focusedLeadingIconColor = hintColor,
            unfocusedLeadingIconColor = hintColor,
            disabledLeadingIconColor = disabledTextColor,
            focusedTrailingIconColor = hintColor,
            unfocusedTrailingIconColor = hintColor,
            disabledTrailingIconColor = disabledTextColor,
            focusedPlaceholderColor = hintColor,
            unfocusedPlaceholderColor = hintColor,
            disabledPlaceholderColor = disabledTextColor,
          ),
      ),
  )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun RecipeSearchBarInputField(
  searchText: TextFieldState,
  searchBarState: SearchBarState,
  onSearch: (String) -> Unit,
  colors: AppBarWithSearchColors,
  onEscape: () -> Boolean,
  modifier: Modifier = Modifier,
) {
  SearchBarDefaults.InputField(
    modifier = modifier.onPreviewKeyEvent { isEscapeShortcut(it) && onEscape() },
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
      if (isCupertino) {
        // iOS clears a field from inside it and submits from the keyboard's Search key, so the
        // trailing slot holds a clear button rather than the checkmark -- which would in any case
        // be a second way out of the field, next to the Cancel button beside it.
        if (searchText.text.isNotEmpty()) {
          IconButton(
            onClick = searchText::clearText,
            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
          ) {
            Icon(
              painter = painterResource(Res.drawable.cancel_24px),
              contentDescription = stringResource(Res.string.clear_search),
            )
          }
        }
      } else if (searchBarState.currentValue == SearchBarValue.Expanded) {
        // One predicate feeding both, so the cursor cannot promise a click the button will not
        // take: a hand over a disabled control reads as an unresponsive app rather than a
        // disabled one.
        val canSubmit = searchText.text.length >= MIN_SEARCH_LENGTH
        IconButton(
          { onSearch(searchText.text.toString()) },
          enabled = canSubmit,
          modifier =
            Modifier.pointerHoverIcon(if (canSubmit) PointerIcon.Hand else PointerIcon.Default),
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
