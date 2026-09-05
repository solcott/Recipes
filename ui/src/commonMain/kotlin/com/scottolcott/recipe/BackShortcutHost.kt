package com.scottolcott.recipe

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

/**
 * Lets a platform window drive the app's back navigation without reaching into the composition.
 *
 * Desktop needs this because it has no system back of any kind:
 * `GestureNavigationDecorationFactory` is a passthrough on JVM, and there is no browser history to
 * fall back on. Keyboard shortcuts are the only non-pointer way back.
 *
 * Handled at the window rather than with a `Modifier.onKeyEvent` on the scaffold: a modifier only
 * sees keys that bubble up from a focused descendant, and nothing holds focus at launch, so the
 * shortcut would silently do nothing until the user had clicked something first.
 *
 * Web deliberately does not use this — the browser already maps these chords to history back, which
 * `BrowserHistoryEffect` turns into navigation. Handling them again would pop twice.
 */
class BackShortcutHost {
  internal var onBack: (() -> Boolean)? = null

  /**
   * Pops one screen. Returns whether anything moved, so the caller can leave the key unconsumed.
   */
  fun requestBack(): Boolean = onBack?.invoke() == true
}

/**
 * The host the running window installed, or `null` where the platform supplies its own back.
 *
 * A composition local because `RecipeScaffoldScreen` is built by Circuit from its `Screen`, so
 * there is no parameter list to thread a host down through.
 */
val LocalBackShortcutHost = staticCompositionLocalOf<BackShortcutHost?> { null }

/**
 * Whether [event] is a "go back" chord: `Cmd+[` or `Cmd+←` (macOS), `Alt+←` (Windows and Linux).
 *
 * All three are accepted on every desktop OS rather than branching on `os.name`. The chords do not
 * collide across platforms, and the alternative would mean a platform source set for a two-line
 * difference.
 *
 * Note this is deliberately not `Backspace`: it is a back chord in older browsers, but it is also
 * the key a user presses in the search field.
 */
fun isBackShortcut(event: KeyEvent): Boolean {
  if (event.type != KeyEventType.KeyDown) return false
  return when (event.key) {
    Key.LeftBracket -> event.isMetaPressed
    Key.DirectionLeft -> event.isMetaPressed || event.isAltPressed
    else -> false
  }
}

/**
 * Whether [event] is the Escape key going down.
 *
 * Not handled at the window like the back chords: the expanded search bar is a `ComposeSceneLayer`
 * of its own, so keys typed into it never reach the main window. `SearchScreen` handles Escape on
 * the input field instead, which holds focus whenever the bar is open.
 */
fun isEscapeShortcut(event: KeyEvent): Boolean =
  event.type == KeyEventType.KeyDown && event.key == Key.Escape
