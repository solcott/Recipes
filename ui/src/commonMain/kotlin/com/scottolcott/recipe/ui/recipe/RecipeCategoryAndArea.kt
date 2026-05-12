package com.scottolcott.recipe.ui.recipe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.scottolcott.recipe.domain.presenter.RecipeDetailsEvent
import com.scottolcott.recipe.model.Recipe
import com.scottolcott.recipe.ui.Res
import com.scottolcott.recipe.ui.label_24px
import com.scottolcott.recipe.ui.location_on_24px
import org.jetbrains.compose.resources.painterResource

@Composable
fun RecipeCategoryAndArea(
  recipe: Recipe,
  eventSink: (RecipeDetailsEvent) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    recipe.category?.let { category ->
      AssistChip(
        onClick = { eventSink(RecipeDetailsEvent.CategoryClicked(category)) },
        label = { Text(category) },
        leadingIcon = { Icon(painterResource(Res.drawable.label_24px), null) },
        colors =
          AssistChipDefaults.assistChipColors(
            labelColor = MaterialTheme.colorScheme.primary,
            leadingIconContentColor = MaterialTheme.colorScheme.tertiary,
          ),
        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand, true),
      )
    }
    recipe.area?.let { area ->
      AssistChip(
        onClick = { eventSink(RecipeDetailsEvent.AreaClicked(area)) },
        label = { Text(area) },
        leadingIcon = { Icon(painterResource(Res.drawable.location_on_24px), null) },
        colors =
          AssistChipDefaults.assistChipColors(
            labelColor = MaterialTheme.colorScheme.primary,
            leadingIconContentColor = MaterialTheme.colorScheme.tertiary,
          ),
        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand, true),
      )
    }
  }
}
