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
 * This dims the *content*, which is what iOS controls, icons and cards do. Table rows are the one
 * thing it does not cover: those fill their *background* with grey and leave the content alone.
 * Nothing in this app is a grouped table yet, so that second treatment is left unwritten rather
 * than kept warm unused -- it belongs with the list section that would opt into it.
 *
 * The alpha is a constructor parameter rather than a theme read, because a `Modifier.Node` has no
 * composition to read from. The theme supplies it at the point of install.
 */
@Immutable
internal data class CupertinoFadeIndication(private val pressedAlpha: Float = DEFAULT_FADE_ALPHA) :
  IndicationNodeFactory {
  override fun create(interactionSource: InteractionSource): DelegatableNode =
    FadeNode(interactionSource, pressedAlpha)
}

private const val DEFAULT_FADE_ALPHA = 0.4f
private const val PRESS_IN_MILLIS = 90
private const val PRESS_OUT_MILLIS = 260

/**
 * Fades the content it is attached to while the pointer is held.
 *
 * Nested press/release pairs drive [progress] between 0 (idle) and 1 (held). The press-in is quick
 * and the release slow, matching how iOS snaps to the pressed state but eases back out. Counting
 * presses rather than using a boolean keeps a release from clearing the state while a second
 * pointer is still down.
 */
private class FadeNode(
  private val interactionSource: InteractionSource,
  private val pressedAlpha: Float,
) : Modifier.Node(), DrawModifierNode {

  private val progress = Animatable(0f)

  // saveLayer rather than a plain overlay: fading the content is not the same as painting
  // translucent paint over it, and only the former reads as an iOS control being pressed.
  private val paint = Paint()

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
