package com.scottolcott.recipe

import androidx.compose.runtime.Composable
import com.scottolcott.recipe.domain.presenter.HomeScreen
import com.scottolcott.recipe.domain.presenter.HomeTabScreen
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.navigation.NavStack

/**
 * Synchronises Circuit's inner navigation stack with the browser's History API so that the browser
 * back/forward buttons drive Circuit navigation on web targets.
 *
 * On non-web platforms this is a no-op.
 */
@Composable
expect fun BrowserHistoryEffect(navStack: NavStack<out NavStack.Record>, navigator: Navigator)

/**
 * Reflects the selected [HomeScreen] tab in the browser's address bar so a tab is shareable and
 * reloadable.
 *
 * The tab lives in `HomePresenter` state rather than in the nav stack — `NavStack` has no
 * replace-top operation, and routing tab changes through `goTo` would add a history entry per pager
 * swipe — so [BrowserHistoryEffect] never sees it. This rewrites the *current* entry instead of
 * pushing one, which adds no history and fires no `popstate`, leaving that effect's depth
 * bookkeeping untouched.
 *
 * The nav record therefore keeps whatever tab it was created with while the address bar tracks the
 * live one. That divergence is harmless: pops move via `history.go`, restoring the URL stamped on
 * the target entry, and the presenter restores the matching tab from its own saved state.
 *
 * On non-web platforms this is a no-op.
 */
@Composable expect fun BrowserTabUrlEffect(tab: HomeTabScreen)
