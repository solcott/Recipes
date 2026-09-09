package com.scottolcott.recipe.ui.area

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import com.scottolcott.recipe.domain.LocalWindowSizeClass
import com.scottolcott.recipe.domain.presenter.AreasEvent
import com.scottolcott.recipe.domain.presenter.AreasScreen
import com.scottolcott.recipe.domain.presenter.AreasState
import com.scottolcott.recipe.model.Area
import com.scottolcott.recipe.ui.ErrorDisplay
import com.scottolcott.recipe.ui.Res
import com.scottolcott.recipe.ui.design.AppCard
import com.scottolcott.recipe.ui.isShortWindow
import com.scottolcott.recipe.ui.no_areas_found
import com.scottolcott.recipe.ui.rememberAdaptiveGridCells
import com.scottolcott.recipe.ui.rememberAdaptivePadding
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.zacsweers.metro.AppScope
import org.jetbrains.compose.resources.stringResource

@Suppress("unused")
@Composable
@CircuitInject(AreasScreen::class, AppScope::class)
fun AreasScreen(state: AreasState, modifier: Modifier = Modifier) {
  val cells = rememberAdaptiveGridCells(targetWidth = 165.dp, shortWindowTargetWidth = 200.dp)
  val padding = rememberAdaptivePadding()
  // A short window is starved of vertical space even when it is wide -- a landscape phone -- so the
  // larger label is held back for windows that are both wide and tall.
  val roomyLabel =
    LocalWindowSizeClass.current.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND) &&
      !isShortWindow()
  val areaTextStyle =
    if (roomyLabel) {
      MaterialTheme.typography.titleMediumEmphasized
    } else {
      MaterialTheme.typography.titleSmallEmphasized
    }
  // The country is the same word again for most of the list -- Algerian, Algeria -- so it reads as
  // the gloss it is rather than a second title.
  val countryTextStyle = MaterialTheme.typography.bodyMedium
  Box(modifier, contentAlignment = Alignment.TopCenter) {
    when (state) {
      is AreasState.Error ->
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          ErrorDisplay(onRetryClick = { state.eventSink(AreasEvent.Error.RetryClicked) })
        }

      AreasState.Loading ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator()
        }

      is AreasState.Success -> {
        if (state.areas.isEmpty()) {
          Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(Res.string.no_areas_found))
          }
        } else {
          LazyVerticalGrid(
            cells,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = padding,
          ) {
            items(state.areas, key = { it.area }, contentType = { "area_item" }) { area ->
              AreaItem(
                area,
                areaTextStyle = areaTextStyle,
                countryTextStyle = countryTextStyle,
                { state.eventSink(AreasEvent.Success.AreaClicked(area.area)) },
                Modifier.animateItem().pointerHoverIcon(PointerIcon.Hand, true),
              )
            }
          }
        }
      }
    }
  }
}

private val CARD_HORIZONTAL_PADDING = 12.dp
private val CARD_VERTICAL_PADDING = 10.dp

/** Lines reserved for each label. Both shrink to fit them rather than drop the words that spill. */
private const val LABEL_LINES = 2

/**
 * How far each label may shrink before it gives up and ellipsizes.
 *
 * The country goes smaller than the area does: it is the secondary line, and it holds the longest
 * strings in the list -- `Saint Vincent and the Grenadines` against an area of `Vincentian`.
 */
private val AREA_MIN_FONT_SIZE = 11.sp
private val COUNTRY_MIN_FONT_SIZE = 9.sp

/** Every typography set here specifies an sp line height; the fallback is belt and braces. */
private val TextStyle.resolvedLineHeight: TextUnit
  get() = if (lineHeight.isSp) lineHeight else fontSize

@Composable
fun AreaItem(
  area: Area,
  areaTextStyle: TextStyle,
  countryTextStyle: TextStyle,
  onAreaClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  // An areas card has no image to give it a size, so its height comes from its type: two lines of
  // each label, at whatever the design language and the window have made those lines. Fixing it
  // here is what keeps a row flush -- LazyVerticalGrid sizes a row to its tallest card and
  // top-aligns the rest, so a card left to size itself would hang a gap under every short name.
  //
  // Each label is given two lines of *its own* leading rather than half the card. The two styles
  // do not share a line height, so an even split hands the larger one less than the two lines it
  // was promised, and it ellipsizes on a card that has the room for it.
  val density = LocalDensity.current
  val areaHeight = with(density) { areaTextStyle.resolvedLineHeight.toDp() * LABEL_LINES }
  val countryHeight = with(density) { countryTextStyle.resolvedLineHeight.toDp() * LABEL_LINES }
  AppCard(
    onClick = onAreaClick,
    modifier = modifier.height(areaHeight + countryHeight + CARD_VERTICAL_PADDING * 2),
  ) {
    Column(
      Modifier.fillMaxSize()
        .padding(horizontal = CARD_HORIZONTAL_PADDING, vertical = CARD_VERTICAL_PADDING)
    ) {
      // Bottom- then top-aligned so a one-line pair meets in the middle of the card rather than
      // floating apart at its two ends, while a two-line pair still fills it.
      Box(Modifier.height(areaHeight).fillMaxWidth(), contentAlignment = Alignment.BottomStart) {
        FittedLabel(area.area, areaTextStyle, AREA_MIN_FONT_SIZE)
      }
      Box(
        Modifier.height(countryHeight).fillMaxWidth(),
        contentAlignment = Alignment.TopStart,
      ) {
        FittedLabel(
          area.country,
          countryTextStyle,
          COUNTRY_MIN_FONT_SIZE,
          MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

/**
 * A label that shrinks to fit [LABEL_LINES] rather than dropping the words that do not fit.
 *
 * The ellipsis is the floor's safety net: below [minFontSize] the text stops shrinking, and a label
 * that still does not fit says so rather than clipping mid-word.
 */
@Composable
private fun FittedLabel(
  text: String,
  style: TextStyle,
  minFontSize: TextUnit,
  color: Color = Color.Unspecified,
) {
  Text(
    text,
    color = color,
    autoSize = TextAutoSize.StepBased(minFontSize = minFontSize, maxFontSize = style.fontSize),
    overflow = TextOverflow.Ellipsis,
    maxLines = LABEL_LINES,
    style = style,
  )
}
