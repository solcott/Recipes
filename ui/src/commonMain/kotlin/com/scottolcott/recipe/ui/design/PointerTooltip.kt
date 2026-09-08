package com.scottolcott.recipe.ui.design

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import com.scottolcott.recipe.domain.isPointer

/**
 * Names an icon-only control on hover, where there is a pointer to hover with.
 *
 * The bar's actions are glyphs with nothing beside them. A screen reader has always had the name --
 * every one of them carries a `contentDescription` -- but a sighted mouse user had no way to ask,
 * and on a desktop that is a gap rather than a nicety: the app bar is the only chrome the window
 * has. [text] should be the same string the anchor gives its `contentDescription`, so the two
 * cannot drift.
 *
 * No `modifier` parameter, and `internal` so nothing outside `:ui` expects one: this wraps its
 * anchor rather than laying anything out, so there is nothing for a caller's modifier to apply to
 * that the anchor could not take directly.
 *
 * A no-op under [com.scottolcott.recipe.domain.AppInput.Touch]. `TooltipBox` would fall back to
 * long-press there, which collides with nothing today but is a gesture this app does not otherwise
 * use, and which no touch user would think to try on an icon.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PointerTooltip(text: String, content: @Composable () -> Unit) {
  if (!isPointer) {
    content()
    return
  }
  TooltipBox(
    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
    tooltip = { PlainTooltip { Text(text) } },
    state = rememberTooltipState(),
    content = content,
  )
}
