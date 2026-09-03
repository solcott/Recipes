package com.scottolcott.recipe.domain.presenter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.serialization.CircuitSerializable
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.redacted.annotations.Redacted
import kotlinx.serialization.Serializable

/**
 * The tabs shown by [HomeScreen], in display order.
 *
 * A compile-time constant, so it needs neither `remember` nor `retain`. The order is the pager's
 * page order.
 */
private val HOME_TABS: List<HomeTabScreen> =
  listOf(CategoriesScreen, IngredientsScreen, AreasScreen)

@CircuitInject(HomeScreen::class, AppScope::class)
@Inject
class HomePresenter internal constructor(private val navigator: Navigator) : Presenter<HomeState> {
  @Composable
  override fun present(): HomeState {
    // Deliberately rememberSerializable rather than the retain{} this repo uses elsewhere: the
    // selected tab is cheap to persist and worth keeping across process death, so reopening the
    // app lands on the tab the user left.
    var selectedTab: HomeTabScreen by
      rememberSerializable(stateSerializer = HomeTabScreen.serializer()) {
        mutableStateOf(CategoriesScreen)
      }
    // indexOf returns -1 for a tab that is no longer in HOME_TABS, which a persisted selection can
    // outlive; fall back to the first tab rather than feeding -1 to the tab row and pager.
    val selectedIndex = remember(selectedTab) { HOME_TABS.indexOf(selectedTab).coerceAtLeast(0) }

    fun eventSink(event: HomeEvent) {
      when (event) {
        is HomeEvent.TabSelected -> selectedTab = event.tab
      }
    }
    return HomeState(selectedTab, selectedIndex, HOME_TABS, navigator, ::eventSink)
  }
}

data class HomeState(
  val selectedTabScreen: HomeTabScreen,
  val selectedIndex: Int,
  val tabScreens: List<HomeTabScreen>,
  val navigator: Navigator,
  @Redacted val eventSink: (HomeEvent) -> Unit,
) : CircuitUiState

sealed interface HomeEvent : CircuitUiEvent {
  data class TabSelected(val tab: HomeTabScreen) : HomeEvent
}

/**
 * A tab hosted by [HomeScreen].
 *
 * `@Serializable` on the sealed interface generates a closed serializer over the three tab objects,
 * which is what persists the selection. Deliberately *not* `@Polymorphic`: that would force open
 * polymorphism and require a `SerializersModule` registration nothing provides.
 */
@Serializable sealed interface HomeTabScreen : Screen

@CircuitSerializable(AppScope::class) data object HomeScreen : Screen
