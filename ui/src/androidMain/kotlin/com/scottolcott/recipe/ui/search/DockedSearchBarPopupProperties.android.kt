package com.scottolcott.recipe.ui.search

import androidx.compose.ui.window.PopupProperties

internal actual fun dockedSearchBarPopupProperties(): PopupProperties =
  PopupProperties(focusable = true, clippingEnabled = false)
