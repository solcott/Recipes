package com.scottolcott.recipe.ui.design

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * An iOS segmented control: a grey track with a single light pill that slides to the selection.
 *
 * Hand-rolled rather than restyled from `SingleChoiceSegmentedButtonRow`, which builds a
 * *connected* row -- rounded only at the two ends, square in between, each segment carrying its own
 * border and a check icon when active. Apple's control is the opposite shape: one floating pill
 * inside a track. Overriding the M3 one to that shape means fighting its border, its icon slot and
 * its per-item shapes, which is more code than the layout below and still animates nothing.
 *
 * The pill is positioned from [BoxWithConstraints]'s measured width rather than a weighted layer,
 * because it has to slide continuously between segments rather than jump between slots.
 */
@Composable
fun AppSegmentedControl(
  options: List<String>,
  selectedIndex: Int,
  onSelect: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptics = LocalHapticFeedback.current
  BoxWithConstraints(
    modifier
      .fillMaxWidth()
      .height(CONTROL_HEIGHT)
      .clip(RoundedCornerShape(TRACK_CORNER))
      .background(MaterialTheme.colorScheme.surfaceVariant)
      .padding(TRACK_PADDING)
  ) {
    val segmentWidth = maxWidth / options.size
    val pillOffset by animateDpAsState(segmentWidth * selectedIndex, label = "segmentedPill")
    Box(
      Modifier.offset(x = pillOffset)
        .width(segmentWidth)
        .fillMaxHeight()
        .clip(RoundedCornerShape(PILL_CORNER))
        .background(MaterialTheme.colorScheme.surface)
    )
    Row(Modifier.fillMaxSize()) {
      options.forEachIndexed { index, label ->
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier.weight(1f)
              .fillMaxHeight()
              .clickable(role = Role.Tab) {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                onSelect(index)
              }
              .pointerHoverIcon(PointerIcon.Hand),
        ) {
          Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
          )
        }
      }
    }
  }
}

private val CONTROL_HEIGHT = 32.dp
private val TRACK_CORNER = 9.dp
private val PILL_CORNER = 7.dp
private val TRACK_PADDING = 2.dp
