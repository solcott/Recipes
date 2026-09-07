package com.scottolcott.recipe.ui.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.scottolcott.recipe.domain.isCupertino
import com.slack.circuit.runtime.screen.Screen
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The app's bottom tab bar, in whichever design language is in force.
 *
 * This exists because iOS had no persistent navigation at all: the rail was suppressed at every
 * window size, leaving a heart icon in the top app bar as the only route between sections. A tab
 * bar is what an iPhone user reaches for, and it is also what makes a root swap safe below the
 * rail's breakpoint -- see `Navigator.selectDestination`, which previously refused to swap roots
 * precisely because nothing else could get the user back.
 *
 * The Cupertino rendering is a floating capsule inset from the screen edges, which is the iOS 26
 * shape; the Material one is a plain `NavigationBar`. Both take the same [destinations], so the
 * caller does not branch.
 */
@Composable
fun AppNavigationBar(
  destinations: List<AppDestination>,
  selected: Screen?,
  onSelect: (Screen) -> Unit,
  modifier: Modifier = Modifier,
) {
  if (isCupertino) {
    CupertinoTabBar(destinations, selected, onSelect, modifier)
  } else {
    MaterialNavigationBar(destinations, selected, onSelect, modifier)
  }
}

@Composable
private fun MaterialNavigationBar(
  destinations: List<AppDestination>,
  selected: Screen?,
  onSelect: (Screen) -> Unit,
  modifier: Modifier = Modifier,
) {
  NavigationBar(modifier) {
    destinations.forEach { destination ->
      val isSelected = destination.isSelectedBy(selected)
      NavigationBarItem(
        selected = isSelected,
        onClick = { onSelect(destination.screen) },
        icon = {
          Icon(
            painter = painterResource(destination.iconFor(isSelected)),
            contentDescription = null,
          )
        },
        label = { Text(stringResource(destination.label)) },
        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
      )
    }
  }
}

/**
 * The iOS 26 tab bar: a capsule floating over the content rather than a band welded to the edge.
 *
 * Laid out by hand rather than by restyling `NavigationBar`, which owns its own height, indicator
 * pill and label behaviour -- overriding all three costs more than the two rows below.
 *
 * **The geometry is measured, and it closes on integers.** Every dimension here comes from the iOS
 * Files app on a 402pt-wide screen: three [TAB_ITEM_WIDTH] slots, [TAB_BAR_PADDING] at each end,
 * and a pill that overhangs its slot by [TAB_PILL_OVERHANG] on each side. That the arithmetic works
 * out exactly (`2 * 8 + 3 * 86 = 274`, the measured bar width) is the sign this is Apple's own
 * model rather than a rounding of it -- so change these as a set, not one at a time.
 *
 * **The slot width is fixed on purpose.** Apple gives every tab the same width and centres its
 * content in it: "Recents", "Shared" and "Browse" are different lengths yet sit on an even 86pt
 * pitch. A fixed width is therefore not an approximation of the target, it is the target -- and it
 * is what makes the pill's offset a known `Dp` rather than something that has to be measured, which
 * is what lets the pill slide with a plain `animateDpAsState`. The cost is that a long enough label
 * ellipsises; the fix for that would be a `SubcomposeLayout` taking the widest item as the pitch,
 * **not** a wider [TAB_ITEM_WIDTH], which would break the arithmetic above.
 *
 * **The pill's corner is concentric with the capsule's, not a radius of its own.**
 * [TAB_PILL_CORNER] is the bar's radius less the gap between the two, so the cap circles share a
 * centre and the gap stays [TAB_PILL_INSET] the whole way round -- Apple's nested-corner rule, and
 * for these dimensions it makes the pill a capsule too (`31 - 4 == 54 / 2`). A smaller radius does
 * not merely look boxy: on the first and last tab the pill's outer corners then fall outside the
 * bar's own curve and `clip` slices them off, which is what gives the shape away.
 *
 * **The pill is a sibling of the items, not a child of one, and that is load-bearing.**
 * [CupertinoFadeIndication] dims via `saveLayer` over the node it is attached to, so a pill nested
 * inside an item's `selectable` would be dimmed along with the glyph on press. iOS dims the glyph
 * and leaves the pill solid, which is what this arrangement gets for free. Do not "simplify" the
 * pill into the item.
 */
@Composable
private fun CupertinoTabBar(
  destinations: List<AppDestination>,
  selected: Screen?,
  onSelect: (Screen) -> Unit,
  modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape(TAB_BAR_CORNER)
  val selectedIndex = destinations.indexOfFirst { it.isSelectedBy(selected) }
  // Holds the last resolved index so a selection that matches no destination parks the pill where
  // it is, rather than sliding it home to the first tab and back.
  var lastSelectedIndex by remember { mutableIntStateOf(0) }
  if (selectedIndex >= 0) lastSelectedIndex = selectedIndex
  val pillOffset by
    animateDpAsState(TAB_ITEM_WIDTH * lastSelectedIndex - TAB_PILL_OVERHANG, label = "tabBarPill")

  Box(
    modifier
      .windowInsetsPadding(tabBarInsets())
      .padding(top = TAB_BAR_TOP_GAP)
      // Take the whole slot, then re-measure the capsule against a relaxed minimum and centre it --
      // the same idiom as `Modifier.maxContentWidth`. A bare `fillMaxWidth` here is what used to
      // stretch the bar across the window; hugging its three tabs is what makes it read as iOS.
      .fillMaxWidth()
      .wrapContentWidth(Alignment.CenterHorizontally)
      .height(TAB_BAR_HEIGHT)
      .clip(shape)
      .background(MaterialTheme.colorScheme.surface)
      .border(Dp.Hairline, MaterialTheme.colorScheme.outlineVariant, shape)
      .padding(horizontal = TAB_BAR_PADDING)
  ) {
    // Drawn before the row, so it sits under the icons and labels. Its vertical inset comes from
    // its own height rather than from padding the box, which keeps each item the full height of
    // the bar and so keeps the whole slot tappable.
    if (selectedIndex >= 0) {
      Box(
        Modifier.align(Alignment.CenterStart)
          .offset(x = pillOffset)
          .size(TAB_PILL_WIDTH, TAB_PILL_HEIGHT)
          .clip(RoundedCornerShape(TAB_PILL_CORNER))
          .background(MaterialTheme.colorScheme.surfaceVariant)
      )
    }
    Row(Modifier.fillMaxHeight()) {
      destinations.forEach { destination ->
        CupertinoTabItem(
          destination = destination,
          selected = destination.isSelectedBy(selected),
          onSelect = { onSelect(destination.screen) },
        )
      }
    }
  }
}

/**
 * How far the capsule sits from the window edges.
 *
 * `max(safeBottom - 12dp, 16dp)`. Apple lets the bar reach *into* the bottom safe area -- Files
 * leaves 22pt of a 34pt inset -- because the home indicator is a 5pt glyph sitting 8pt off the
 * edge, not a 34pt obstacle. Clawing back a fixed 12dp reproduces that on a device with an
 * indicator while the 16dp floor keeps the capsule floating rather than flush on a device that
 * reports no inset at all, which is every desktop and web window.
 */
@Composable
private fun tabBarInsets(): WindowInsets =
  WindowInsets.safeDrawing
    .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
    .exclude(WindowInsets(bottom = TAB_BAR_SAFE_AREA_OVERLAP))
    .union(WindowInsets(bottom = TAB_BAR_MIN_BOTTOM_GAP))

@Composable
private fun CupertinoTabItem(
  destination: AppDestination,
  selected: Boolean,
  onSelect: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptics = LocalHapticFeedback.current
  // Animated so the colour crossfades on the same beat as the pill slides under it. Unselected is
  // the full label colour rather than the secondary grey: iOS stopped dimming tab items once the
  // pill took over signalling selection, and dimming both leaves two of three tabs reading as
  // disabled.
  val tint by
    animateColorAsState(
      if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
      label = "tabItemTint",
    )
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(TAB_LABEL_GAP, Alignment.CenterVertically),
    modifier =
      modifier
        // Sized before `selectable`, so the whole slot is the touch target rather than just the
        // glyph and its label.
        .width(TAB_ITEM_WIDTH)
        .fillMaxHeight()
        .selectable(
          selected = selected,
          role = Role.Tab,
          onClick = {
            // iOS ticks on a tab change. Compose maps this onto the platform's own generator, so it
            // is a no-op rather than a wrong buzz where none exists.
            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            onSelect()
          },
        )
        .pointerHoverIcon(PointerIcon.Hand),
  ) {
    Icon(
      painter = painterResource(destination.iconFor(selected)),
      contentDescription = null,
      tint = tint,
      modifier = Modifier.size(TAB_ICON_SIZE),
    )
    Text(
      text = stringResource(destination.label),
      // The same weight in both states. Emphasising the selected label would reflow the string
      // underneath a pill that is still sliding, which reads as a jiggle rather than as emphasis.
      style = MaterialTheme.typography.labelSmallEmphasized,
      color = tint,
      maxLines = 1,
      softWrap = false,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.Center,
    )
  }
}

private val TAB_BAR_HEIGHT = 62.dp
/** A true capsule, stated as the relationship so it cannot drift from the height. */
private val TAB_BAR_CORNER = TAB_BAR_HEIGHT / 2
/** One slot per destination; the pitch Apple's own bar uses regardless of label length. */
private val TAB_ITEM_WIDTH = 86.dp
private val TAB_PILL_WIDTH = 94.dp
private val TAB_PILL_HEIGHT = 54.dp
/** The gap between pill and capsule, which is the same on all four sides. */
private val TAB_PILL_INSET = (TAB_BAR_HEIGHT - TAB_PILL_HEIGHT) / 2
/** Concentric with the capsule -- see [CupertinoTabBar]. Works out to a capsule of its own here. */
private val TAB_PILL_CORNER = TAB_BAR_CORNER - TAB_PILL_INSET
/** How far the pill spills past its slot on each side, and so how far the first one starts back. */
private val TAB_PILL_OVERHANG = (TAB_PILL_WIDTH - TAB_ITEM_WIDTH) / 2
/** The pill's inset plus its overhang: `2 * 8 + 3 * 86 = 274`, the measured bar width. */
private val TAB_BAR_PADDING = TAB_PILL_INSET + TAB_PILL_OVERHANG
private val TAB_BAR_TOP_GAP = 8.dp
private val TAB_BAR_SAFE_AREA_OVERLAP = 12.dp
private val TAB_BAR_MIN_BOTTOM_GAP = 16.dp
/** 28dp of box gives a 23dp glyph: the drawables carry an 80..880 live area on a 960 viewport. */
private val TAB_ICON_SIZE = 28.dp
private val TAB_LABEL_GAP = 2.dp
