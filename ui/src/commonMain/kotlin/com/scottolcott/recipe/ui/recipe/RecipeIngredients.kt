package com.scottolcott.recipe.ui.recipe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scottolcott.recipe.model.Recipe
import com.scottolcott.recipe.ui.Res
import com.scottolcott.recipe.ui.ingredient_with_measure
import com.scottolcott.recipe.ui.ingredients
import org.jetbrains.compose.resources.stringResource

@Composable
fun RecipeIngredients(recipe: Recipe, modifier: Modifier = Modifier) {
  Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(stringResource(Res.string.ingredients), style = MaterialTheme.typography.titleMedium)
    val ingredients = recipe.details?.ingredients.orEmpty()
    ingredients.forEach {
      Row(
        modifier = Modifier.padding(start = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text("•", style = MaterialTheme.typography.bodyMedium)
        Text(
          stringResource(Res.string.ingredient_with_measure, it.measure, it.ingredient),
          style = MaterialTheme.typography.bodyMedium,
        )
      }
    }
  }
}
