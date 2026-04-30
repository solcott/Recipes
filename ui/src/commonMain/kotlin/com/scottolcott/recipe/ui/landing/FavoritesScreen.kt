package com.scottolcott.recipe.ui.landing

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.scottolcott.recipe.domain.presenter.FavoritesScreen
import com.scottolcott.recipe.domain.presenter.FavoritesState
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.zacsweers.metro.AppScope

@Composable
@CircuitInject(FavoritesScreen::class, AppScope::class)
fun FavoritesScreen(state: FavoritesState, modifier: Modifier = Modifier) {
  LazyColumn(modifier) { items(state.recipes, { it }, { "favorite" }) { Text(it.id) } }
}
