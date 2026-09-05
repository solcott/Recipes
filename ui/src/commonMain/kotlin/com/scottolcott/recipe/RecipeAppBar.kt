package com.scottolcott.recipe

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.scottolcott.recipe.domain.LocalWindowSizeClass
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
import com.scottolcott.recipe.ui.favorite_24px_filled
import com.scottolcott.recipe.ui.favorites
import com.scottolcott.recipe.ui.recipes
import com.scottolcott.recipe.ui.search
import com.scottolcott.recipe.ui.search_24px
import com.scottolcott.recipe.ui.title
import com.slack.circuit.subcircuit.SubCircuitContent
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Caps the back button's label so a long category cannot squeeze the search field beside it. */
private val BackLabelMaxWidth = 160.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeAppBar(state: RecipeScaffoldState, modifier: Modifier = Modifier) {
  AnimatedContent(state.searchVisible) { isActive ->
    if (isActive) {
      val initialSearchBarValue = remember {
        if (state.showNavRail) SearchBarValue.Collapsed else SearchBarValue.Expanded
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
          modifier = modifier.padding(vertical = if (state.showNavRail) 8.dp else 4.dp),
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
  val screenTitle =
    state.navStack.currentRecord?.screen?.title() ?: stringResource(Res.string.recipes)
  val title = @Composable { Text(screenTitle, maxLines = 1, overflow = TextOverflow.Ellipsis) }
  // No navigation rail exists on this layout, so the app mark still earns the slot when there is
  // nowhere to go back to.
  val navigationIcon =
    @Composable { if (showBackButton(state)) BackButton(state) else AppMarkIcon() }
  val actions: @Composable RowScope.() -> Unit = {
    FavoritesAction(state)
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
private fun FavoritesAction(state: RecipeScaffoldState) {
  IconButton(
    onClick = { state.eventSink(RecipeScaffoldEvent.GoTo(RecipesScreen.Favorites)) },
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
 * The back control, labelled with the destination (`< Categories`) wherever there is horizontal
 * room.
 *
 * A bare arrow is easy to miss next to a search field, and on the platforms with no system back --
 * desktop above all -- it is the only way out of a screen. Below the medium-width breakpoint the
 * label is dropped rather than truncated, since that layout is a phone with a system back of its
 * own.
 *
 * Callers decide *whether* to draw this; see [showBackButton].
 */
@Composable
internal fun BackButton(state: RecipeScaffoldState) {
  val icon = if (isIos()) Res.drawable.arrow_back_ios_24px else Res.drawable.arrow_back_24px
  val isWide =
    LocalWindowSizeClass.current.isWidthAtLeastBreakpoint(
      WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
    )
  val label = if (isWide) state.previousScreen?.title() else null
  val onClick = { state.eventSink(RecipeScaffoldEvent.Back) }
  val backDescription = stringResource(Res.string.back)

  if (label == null) {
    IconButton(
      onClick,
      modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
      colors =
        IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
    ) {
      Icon(painter = painterResource(icon), contentDescription = backDescription)
    }
  } else {
    TextButton(
      onClick,
      // The label names the *previous* screen, so it changes width while the button stays put --
      // Categories, then "Category: Seafood" one level deeper. Expanding the slot only covers the
      // button arriving; this covers every change after that.
      modifier = Modifier.animateContentSize().pointerHoverIcon(PointerIcon.Hand),
      colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
    ) {
      // Only the arrow is described. The label is already visible text, so the button's merged
      // semantics read "Back, Categories" without the two halves repeating each other.
      Icon(
        painter = painterResource(icon),
        contentDescription = backDescription,
        tint = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        label,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 4.dp).widthIn(max = BackLabelMaxWidth),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
