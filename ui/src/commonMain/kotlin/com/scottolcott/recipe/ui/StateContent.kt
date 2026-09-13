package com.scottolcott.recipe.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * [AnimatedContent] for a screen's state, keyed on the state's *type* (`state::class`) rather than
 * the state itself.
 *
 * Moving between types -- Loading to Success, Success to Error -- runs [transitionSpec] as usual. A
 * new state of the *same* type -- a Success with fresh data, a refresh finishing, a favorite
 * toggled -- does not animate: it takes over the slot already on screen, and [content] recomposes
 * in place with it. Keyed on the state itself, as plain [AnimatedContent] is, each of those updates
 * would animate into a freshly composed subtree, and every grid and scroll position inside it would
 * start over at the top.
 *
 * The parameters mirror [AnimatedContent]'s, defaults included. The trade-off is that two states of
 * the same class can never animate between each other; a screen that wants that should call
 * [AnimatedContent] with a key of its own.
 */
@Composable
fun <S : Any> AnimatedStateContent(
  state: S,
  modifier: Modifier = Modifier,
  transitionSpec: AnimatedContentTransitionScope<S>.() -> ContentTransform = {
    (fadeIn(animationSpec = tween(220, delayMillis = 90)) +
        scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)))
      .togetherWith(fadeOut(animationSpec = tween(90)))
  },
  contentAlignment: Alignment = Alignment.TopStart,
  label: String = "AnimatedStateContent",
  content: @Composable AnimatedContentScope.(targetState: S) -> Unit,
) {
  AnimatedContent(
    state,
    modifier,
    transitionSpec = transitionSpec,
    contentAlignment = contentAlignment,
    label = label,
    contentKey = { it::class },
    content = content,
  )
}

/**
 * [content] with a progress bar over its top edge while [isRefreshing].
 *
 * Over it rather than above it, so the content does not jump down and back up as a refresh starts
 * and ends.
 */
@Composable
fun RefreshingContent(
  isRefreshing: Boolean,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  Box(modifier) {
    content()
    AnimatedVisibility(
      isRefreshing,
      Modifier.align(Alignment.TopCenter).fillMaxWidth(),
      enter = fadeIn(),
      exit = fadeOut(),
    ) {
      LinearProgressIndicator(Modifier.fillMaxWidth())
    }
  }
}

@Composable
fun LoadingDisplay(modifier: Modifier = Modifier) {
  Box(modifier, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}
