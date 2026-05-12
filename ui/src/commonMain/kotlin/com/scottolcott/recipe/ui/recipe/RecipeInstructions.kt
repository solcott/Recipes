package com.scottolcott.recipe.ui.recipe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scottolcott.recipe.model.Recipe
import com.scottolcott.recipe.ui.Res
import com.scottolcott.recipe.ui.instructions
import org.jetbrains.compose.resources.stringResource

@Composable
fun RecipeInstructions(recipe: Recipe, modifier: Modifier = Modifier) {
  Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(stringResource(Res.string.instructions), style = MaterialTheme.typography.titleMedium)
    recipe.details?.instructions?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
  }
}
