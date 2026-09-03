package com.scottolcott.recipe.ui.search

import androidx.compose.ui.window.PopupProperties

/**
 * The [PopupProperties] for `ExpandedDockedSearchBar`'s popup.
 *
 * Material3 renders the docked search bar directly on top of the collapsed one: its
 * `PopupPositionProvider` ignores `anchorBounds` and returns
 * `SearchBarState.collapsedBounds.topLeft`, a `positionInWindow()` in **full-window** coordinates.
 * That exact overlap is why `AppBarWithSearch` can leave its own copy of the input field opaque —
 * only the full-screen variants set `SearchBarState.expandsToFullScreen`, the flag that fades the
 * app bar's copy out.
 *
 * Compose's skiko `Popup` assumes the opposite convention. `ComposeSceneLayerMeasurePolicy` states
 * that the "position provider works in coordinates without insets": it hands the provider an
 * inset-relative `anchorBounds` and then adds `platformInsets.left`/`top` back onto whatever comes
 * out. Material3's provider never reads `anchorBounds`, so that addition is pure error — the popup
 * lands one safe-area inset down and to the right of the bar it was meant to cover, and the still
 * opaque app-bar copy shows through as a second search field.
 *
 * On an iPad that is a (20dp, 40dp) offset. iPhone never reaches this code, because it falls below
 * the 600x480dp breakpoint that selects the docked bar over the full-screen one. Opting the popup
 * out of platform insets returns it to full-window coordinates — the space Material3's provider is
 * already working in.
 *
 * `usePlatformInsets` exists only on the skiko `PopupProperties`; Android's `Popup` is a real
 * window positioned against window coordinates already, so it needs no adjustment. The remaining
 * values mirror Material3's own defaults for this popup.
 */
internal expect fun dockedSearchBarPopupProperties(): PopupProperties
