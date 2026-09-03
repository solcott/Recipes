package com.scottolcott.recipe

import androidx.compose.runtime.Composable
import com.scottolcott.recipe.domain.presenter.HomeTabScreen
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.navigation.NavStack

@Composable
actual fun BrowserHistoryEffect(navStack: NavStack<out NavStack.Record>, navigator: Navigator) =
  Unit

@Composable actual fun BrowserTabUrlEffect(tab: HomeTabScreen) = Unit
