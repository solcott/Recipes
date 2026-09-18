package com.scottolcott.recipe.ui.recipe

import androidx.compose.foundation.layout.ExperimentalFlexBoxApi
import androidx.compose.foundation.layout.FlexBox
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalFlexBoxApi::class)
@Composable
fun RecipeTags(tags: ImmutableList<String>, modifier: Modifier = Modifier) {
  if (tags.isNotEmpty()) {
    FlexBox(config = { gap(8.dp) }, modifier = modifier) {
      for (tag in tags) {
        AssistChip(onClick = {}, label = { Text(tag) })
      }
    }
  }
}
