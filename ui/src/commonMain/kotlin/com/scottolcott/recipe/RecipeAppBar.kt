package com.scottolcott.recipe

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import com.scottolcott.recipe.domain.presenter.RecipeScaffoldEvent
import com.scottolcott.recipe.domain.presenter.RecipeScaffoldState
import com.scottolcott.recipe.domain.presenter.RecipesScreen
import com.scottolcott.recipe.domain.presenter.SearchOuterEvent
import com.scottolcott.recipe.domain.presenter.SearchScreen
import com.scottolcott.recipe.ui.Res
import com.scottolcott.recipe.ui.arrow_back_24px
import com.scottolcott.recipe.ui.arrow_back_ios_24px
import com.scottolcott.recipe.ui.chef_hat_24px
import com.scottolcott.recipe.ui.favorite_24px_filled
import com.scottolcott.recipe.ui.recipes
import com.scottolcott.recipe.ui.search_24px
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.navigation.NavStack
import com.slack.circuit.runtime.navigation.canGoBack
import com.slack.circuit.subcircuit.SubCircuitContent
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeAppBar(state: RecipeScaffoldState, modifier: Modifier = Modifier) {
  AnimatedContent(state.isSearchActive) { isActive ->
    if (isActive) {
      SubCircuitContent(
        SearchScreen,
        outerEventSink = {
          when (it) {
            is SearchOuterEvent.NavigateToSearchResults -> state.eventSink(
              RecipeScaffoldEvent.GoTo(
                RecipesScreen.BySearch(it.query)
              )
            )

            is SearchOuterEvent.NavigateToCategoryResults ->
              state.eventSink(RecipeScaffoldEvent.GoTo(RecipesScreen.ByCategory(it.category.name)))
            is SearchOuterEvent.NavigateToIngredientResults -> {
              state.eventSink(RecipeScaffoldEvent.GoTo(RecipesScreen.ByIngredient(it.ingredient.name)))
            }
          }
        },
      )
    } else {
      RecipeTopAppBar(state, modifier)
    }
  }
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
      onClick = { state.eventSink(RecipeScaffoldEvent.SearchClicked) },
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
