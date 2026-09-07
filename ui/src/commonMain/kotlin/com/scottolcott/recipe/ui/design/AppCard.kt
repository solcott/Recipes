package com.scottolcott.recipe.ui.design

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scottolcott.recipe.domain.isCupertino

/**
 * A tappable card, in whichever design language is in force.
 *
 * The two differ in exactly one thing, and it is the thing that gives Material away: the outline.
 * iOS groups content by rounding it and letting it sit on a darker page, never by drawing a border
 * around each item -- so the Cupertino card is a plain filled surface with no stroke and no
 * elevation, and the grouped background behind it does the separating.
 */
@Composable
fun AppCard(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  if (isCupertino) {
    Card(
      onClick = onClick,
      shape = MaterialTheme.shapes.medium,
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
      modifier = modifier,
      content = content,
    )
  } else {
    OutlinedCard(onClick = onClick, modifier = modifier, content = content)
  }
}
