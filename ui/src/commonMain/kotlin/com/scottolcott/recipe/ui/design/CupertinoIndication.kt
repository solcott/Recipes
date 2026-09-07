package com.scottolcott.recipe.ui.design

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import kotlinx.coroutines.launch

/**
 * iOS press feedback, in place of Material's ripple.
 *
 * The expanding ink ripple is the single most pervasive tell that a Compose app is not native, and
 * because every `clickable`, `IconButton`, `Card` and `ListItem` resolves its indication through
 * `LocalIndication`, swapping that one value removes it everywhere at once.
 *
 * iOS uses two different treatments and they are not interchangeable, so both live here:
 * - [CupertinoFadeIndication] dims the *content*, which is what controls, icons and cards do. This
 *   is the one installed globally.
 * - [CupertinoHighlightIndication] fills the *background* with grey and leaves content alone, which
 *   is what table rows do. [AppListSection] opts into it.
 *
 * Both take their colour as a constructor parameter rather than reading the theme, because a
 * `Modifier.Node` has no composition to read from. The theme supplies it at the point of install.
 */
@Immutable
internal data class CupertinoFadeIndication(private val pressedAlpha: Float = DEFAULT_FADE_ALPHA) :
  IndicationNodeFactory {
  override fun create(interactionSource: InteractionSource): DelegatableNode =
    FadeNode(interactionSource, pressedAlpha)
}

@Immutable
internal data class CupertinoHighlightIndication(private val highlight: Color) :
  IndicationNodeFactory {
  override fun create(interactionSource: InteractionSource): DelegatableNode =
    HighlightNode(interactionSource, highlight)
}

private const val DEFAULT_FADE_ALPHA = 0.4f
private const val HIGHLIGHT_ALPHA = 0.12f
private const val PRESS_IN_MILLIS = 90
private const val PRESS_OUT_MILLIS = 260

/**
 * Tracks nested press/release pairs and drives [progress] between 0 (idle) and 1 (held).
 *
 * The press-in is quick and the release slow, matching how iOS snaps to the pressed state but eases
 * back out. Counting presses rather than using a boolean keeps a release from clearing the state
 * while a second pointer is still down.
 */
private abstract class PressProgressNode(private val interactionSource: InteractionSource) :
  Modifier.Node(), DrawModifierNode {

  protected val progress = Animatable(0f)

  override fun onAttach() {
    coroutineScope.launch {
      var presses = 0
      interactionSource.interactions.collect { interaction ->
        when (interaction) {
          is PressInteraction.Press -> presses++
          is PressInteraction.Release,
          is PressInteraction.Cancel -> presses--
        }
        val held = presses > 0
        launch {
          progress.animateTo(
            targetValue = if (held) 1f else 0f,
            animationSpec = tween(if (held) PRESS_IN_MILLIS else PRESS_OUT_MILLIS),
          )
        }
      }
    }
  }
}

private class FadeNode(interactionSource: InteractionSource, private val pressedAlpha: Float) :
  PressProgressNode(interactionSource) {

  // saveLayer rather than a plain overlay: fading the content is not the same as painting
  // translucent paint over it, and only the former reads as an iOS control being pressed.
  private val paint = Paint()

  override fun ContentDrawScope.draw() {
    val alpha = 1f - (1f - pressedAlpha) * progress.value
    if (alpha >= 1f) {
      drawContent()
      return
    }
    paint.alpha = alpha
    drawContext.canvas.saveLayer(Rect(Offset.Zero, size), paint)
    drawContent()
    drawContext.canvas.restore()
  }
}

private class HighlightNode(interactionSource: InteractionSource, private val highlight: Color) :
  PressProgressNode(interactionSource) {

  override fun ContentDrawScope.draw() {
    val alpha = progress.value * HIGHLIGHT_ALPHA
    if (alpha > 0f) drawRect(color = highlight, alpha = alpha)
    drawContent()
  }
}
