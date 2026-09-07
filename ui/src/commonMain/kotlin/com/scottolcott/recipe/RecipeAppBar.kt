package com.scottolcott.recipe

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.scottolcott.recipe.domain.isCupertino
import com.scottolcott.recipe.domain.presenter.NavigationLayout
import com.scottolcott.recipe.domain.presenter.RecipeScaffoldEvent
import com.scottolcott.recipe.domain.presenter.RecipeScaffoldState
import com.scottolcott.recipe.domain.presenter.RecipesScreen
import com.scottolcott.recipe.domain.presenter.SearchOuterEvent
import com.scottolcott.recipe.domain.presenter.SearchScreen
import com.scottolcott.recipe.ui.Res
import com.scottolcott.recipe.ui.arrow_back_24px
import com.scottolcott.recipe.ui.arrow_back_ios_24px
import com.scottolcott.recipe.ui.back
import com.scottolcott.recipe.ui.chef_hat_24px
import com.scottolcott.recipe.ui.design.LocalTopAppBarScrollBehavior
import com.scottolcott.recipe.ui.favorite_24px_filled
import com.scottolcott.recipe.ui.favorites
import com.scottolcott.recipe.ui.navigationBarTitle
import com.scottolcott.recipe.ui.search
import com.scottolcott.recipe.ui.search_24px
import com.scottolcott.recipe.ui.title
import com.slack.circuit.subcircuit.SubCircuitContent
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeAppBar(state: RecipeScaffoldState, modifier: Modifier = Modifier) {
  AnimatedContent(state.searchVisible) { isActive ->
    if (isActive) {
      val initialSearchBarValue = remember {
        if (state.navigationLayout == NavigationLayout.Rail) {
          SearchBarValue.Collapsed
        } else {
          SearchBarValue.Expanded
        }
      }
      // The search bar stands in for the whole top app bar here, so the back control has to travel
      // with it. Nothing else does: the navigation rail beside it already carries the app mark and
      // Favorites, and repeating them a few pixels apart just reads as clutter. See
      // [LocalAppBarNavigationIcon] for why this arrives as a composition local.
      CompositionLocalProvider(
        LocalAppBarNavigationIcon provides if (isWeb()) null else ({ AnimatedBackButton(state) })
      ) {
        SubCircuitContent(
          SearchScreen(initialSearchBarValue = initialSearchBarValue),
          modifier =
            modifier.padding(
              vertical = if (state.navigationLayout == NavigationLayout.Rail) 8.dp else 4.dp
            ),
          outerEventSink = {
            when (it) {
              is SearchOuterEvent.NavigateToSearchResults -> {
                state.eventSink(RecipeScaffoldEvent.GoTo(RecipesScreen.BySearch(it.query)))
              }
              is SearchOuterEvent.NavigateToCategoryResults -> {
                state.eventSink(
                  RecipeScaffoldEvent.GoTo(RecipesScreen.ByCategory(it.category.name))
                )
              }
              is SearchOuterEvent.NavigateToIngredientResults -> {
                state.eventSink(
                  RecipeScaffoldEvent.GoTo(RecipesScreen.ByIngredient(setOf(it.ingredient.name)))
                )
              }
              is SearchOuterEvent.SearchBarStateChanged ->
                state.eventSink(RecipeScaffoldEvent.SearchBarStateChanged(it.searchBarValue))
            }
          },
        )
      }
    } else {
      RecipeTopAppBar(state, modifier)
    }
  }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun RecipeTopAppBar(state: RecipeScaffoldState, modifier: Modifier = Modifier) {
  // Under Material the bar stays nameless: a screen names itself in its own content, where that
  // name is visible at every window size, and this bar is replaced wholesale by the search bar on a
  // layout wide enough for the navigation rail. Cupertino inverts that -- the nav bar *is* where an
  // iOS user reads where they are -- so the title moves up here and the in-content heading stands
  // down. See `RecipesScreen`.
  val screenTitle =
    if (isCupertino) state.navStack.currentRecord?.screen?.navigationBarTitle() else null
  val title =
    @Composable {
      // `if` rather than `?.let`: the latter makes the lambda return `Unit?`, which the app bar
      // title slot will not take.
      if (screenTitle != null) Text(screenTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
  // No navigation rail exists on this layout, so the app mark still earns the slot when there is
  // nowhere to go back to.
  val navigationIcon =
    @Composable {
      if (showBackButton(state)) {
        BackButton(state)
      } else if (state.navigationLayout != NavigationLayout.BottomBar) {
        // With a tab bar on screen the mark is already down there, and showing it twice a few
        // pixels apart reads as a rendering fault rather than branding -- the same reasoning
        // `AppMarkIcon` already applies to the navigation rail.
        AppMarkIcon()
      }
    }
  val actions: @Composable RowScope.() -> Unit = {
    // Favorites is a tab of its own on that layout; here it would be a second door to one room.
    if (state.navigationLayout != NavigationLayout.BottomBar) FavoritesAction(state)
    // Search is a tab of its own on that layout, so the bar keeps no shortcut to it either.
    if (state.navigationLayout != NavigationLayout.BottomBar) {
      IconButton(
        onClick = { state.eventSink(RecipeScaffoldEvent.SearchClicked) },
        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
      ) {
        Icon(
          painter = painterResource(Res.drawable.search_24px),
          contentDescription = stringResource(Res.string.search),
        )
      }
    }
  }

  when {
    // A tab root gets the large title that collapses as the user scrolls; a pushed screen gets the
    // inline centred one over a back chevron. That split is iOS's own -- Music and Photos both do
    // it -- and it falls out of `canGoBack` without a hand-rolled bar.
    isCupertino && !showBackButton(state) ->
      LargeTopAppBar(
        title = title,
        navigationIcon = navigationIcon,
        actions = actions,
        colors = cupertinoBarColors(),
        scrollBehavior = LocalTopAppBarScrollBehavior.current,
        modifier = modifier,
      )
    isCupertino ->
      CenterAlignedTopAppBar(
        title = title,
        navigationIcon = navigationIcon,
        actions = actions,
        colors = cupertinoBarColors(),
        modifier = modifier,
      )
    else ->
      TopAppBar(
        title = title,
        navigationIcon = navigationIcon,
        actions = actions,
        modifier = modifier,
      )
  }
}

/**
 * Bar colours for the Cupertino designs.
 *
 * `background` rather than a Material surface tint, so the bar reads as part of the page instead of
 * a raised sheet on top of it -- and so it matches the search bar that replaces it, which reaches
 * the same colour by being transparent over the scaffold. See `getAppBarWithSearchColors`.
 *
 * Opaque, and not a floating surface the way the tab bar is: this bar spans the window edges and
 * sits flush on the page colour, so it has no edge of its own to read as raised.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun cupertinoBarColors() =
  TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.background,
    scrolledContainerColor = MaterialTheme.colorScheme.background,
    titleContentColor = MaterialTheme.colorScheme.onSurface,
    actionIconContentColor = MaterialTheme.colorScheme.primary,
    navigationIconContentColor = MaterialTheme.colorScheme.primary,
  )

@Composable
private fun FavoritesAction(state: RecipeScaffoldState) {
  IconButton(
    onClick = { state.eventSink(RecipeScaffoldEvent.SelectDestination(RecipesScreen.Favorites)) },
    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
  ) {
    Icon(
      painter = painterResource(Res.drawable.favorite_24px_filled),
      contentDescription = stringResource(Res.string.favorites),
    )
  }
}

/**
 * Whether the leading slot should offer a way back.
 *
 * Web never does: the browser's own back button sits a few pixels above the page at every window
 * size, and its `Alt+Left` / `Cmd+[` chords already reach the same navigation through
 * [BrowserHistoryEffect]. A second one inside the page is redundant at best and a second source of
 * truth at worst.
 */
private fun showBackButton(state: RecipeScaffoldState) = state.canGoBack && !isWeb()

/**
 * [BackButton] wrapped so it grows and shrinks the slot instead of popping in and out.
 *
 * The search bar sits directly beside this, so the button appearing shoves the whole field sideways
 * -- worth animating rather than cutting. `AnimatedVisibility` has to live *inside* the slot rather
 * than around it: Material3 wraps a non-null `navigationIcon` in its own padded `Box`, so swapping
 * the slot itself between null and a button would still snap.
 */
@Composable
private fun AnimatedBackButton(state: RecipeScaffoldState) {
  AnimatedVisibility(
    showBackButton(state),
    enter = expandHorizontally(),
    exit = shrinkHorizontally(),
  ) {
    BackButton(state)
  }
}

/**
 * The app's mark, standing in for the back button where there is nothing to go back to.
 *
 * A disabled button rather than a plain `Icon` so it occupies the same slot and metrics as the
 * button it replaces, and the title beside it does not shift as the stack changes.
 *
 * Only for layouts with no navigation rail. The rail leads with this same chef-hat, and showing it
 * twice within a few pixels reads as a rendering bug rather than branding.
 */
@Composable
private fun AppMarkIcon() {
  IconButton(
    {},
    enabled = false,
    colors = IconButtonDefaults.iconButtonColors(disabledContentColor = LocalContentColor.current),
  ) {
    Icon(painter = painterResource(Res.drawable.chef_hat_24px), contentDescription = null)
  }
}

/**
 * The back control.
 *
 * On the platforms with no system back -- desktop above all -- this is the only way out of a
 * screen, so it stays a plain icon button rather than anything subtler.
 *
 * Callers decide *whether* to draw this; see [showBackButton].
 */
@Composable
internal fun BackButton(state: RecipeScaffoldState) {
  if (isCupertino) CupertinoBackButton(state) else MaterialBackButton(state)
}

@Composable
private fun MaterialBackButton(state: RecipeScaffoldState) {
  IconButton(
    { state.eventSink(RecipeScaffoldEvent.Back) },
    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
    colors =
      IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
  ) {
    Icon(
      painter = painterResource(Res.drawable.arrow_back_24px),
      contentDescription = stringResource(Res.string.back),
    )
  }
}

/**
 * Chevron plus the name of the screen being returned to, tinted with the accent.
 *
 * This restores the label that commit f9829b6 removed, but only for iOS, and the reason it was
 * dropped does not apply here: it was carrying a name the Material bar already showed nowhere, next
 * to a search bar that needed the width. On iOS the labelled back button is the convention, and it
 * is the only thing in the bar competing for space.
 *
 * The label falls back to a plain "Back" when the previous screen has no name of its own -- a
 * recipe, whose title lives on the record rather than the [Screen].
 */
@Composable
private fun CupertinoBackButton(state: RecipeScaffoldState) {
  val previous = state.navStack.snapshot()?.backwardItems?.firstOrNull()?.screen
  val label = previous?.title() ?: stringResource(Res.string.back)
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier =
      Modifier.clip(MaterialTheme.shapes.small)
        .clickable(onClickLabel = stringResource(Res.string.back)) {
          state.eventSink(RecipeScaffoldEvent.Back)
        }
        .pointerHoverIcon(PointerIcon.Hand)
        .padding(end = BACK_LABEL_END_PADDING),
  ) {
    Icon(
      painter = painterResource(Res.drawable.arrow_back_ios_24px),
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
    )
    Text(
      text = label,
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.primary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.widthIn(max = BACK_LABEL_MAX_WIDTH),
    )
  }
}

private val BACK_LABEL_MAX_WIDTH = 120.dp
private val BACK_LABEL_END_PADDING = 8.dp
