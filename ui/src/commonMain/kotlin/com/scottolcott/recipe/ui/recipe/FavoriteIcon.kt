package com.scottolcott.recipe.ui.recipe

import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.scottolcott.recipe.ui.Res
import com.scottolcott.recipe.ui.favorite_24px
import com.scottolcott.recipe.ui.favorite_24px_filled
import org.jetbrains.compose.resources.painterResource

@Composable
fun FavoriteIcon(
  isFavorite: Boolean,
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
) {
  Icon(
    if (isFavorite) painterResource(Res.drawable.favorite_24px_filled)
    else painterResource(Res.drawable.favorite_24px),
    contentDescription = "favorite",
    modifier =
      modifier.clip(CircleShape).clickable(enabled = onClick != null) { onClick?.invoke() },
    tint = MaterialTheme.colorScheme.primary,
  )
}
