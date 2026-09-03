package com.scottolcott.recipe.domain.presenter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.serialization.CircuitSerializable
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.redacted.annotations.Redacted
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable

@CircuitInject(HomeScreen::class, AppScope::class)
@Inject
class HomePresenter(val navigator: Navigator) : Presenter<HomeState> {
  @Composable
  override fun present(): HomeState {
    var selectedTab: HomeTabScreen by
      rememberSerializable(stateSerializer = HomeTabScreen.serializer()) {
        mutableStateOf(CategoriesScreen)
      }
    val tabs = retain {
      listOf(
        CategoriesScreen,
        IngredientsScreen,
        AreasScreen,
      )
    }
    val selectedIndex = remember(selectedTab, tabs) { tabs.indexOf(selectedTab) }
    fun eventSink(event: HomeEvent) {
      when (event) {
        is HomeEvent.TabSelected -> selectedTab = event.tab
      }
    }
    return HomeState(selectedTab, selectedIndex, tabs, navigator, ::eventSink)
  }
}

data class HomeState(
  val selectedTabScreen: HomeTabScreen,
  val selectedIndex: Int,
  val tabScreens: List<HomeTabScreen>,
  val navigator: Navigator,
  @Redacted val eventSink: (HomeEvent) -> Unit,
) : CircuitUiState

sealed interface HomeEvent {
  data class TabSelected(val tab: HomeTabScreen) : HomeEvent
}

@Polymorphic @Serializable sealed interface HomeTabScreen : Screen

@CircuitSerializable(AppScope::class) data object HomeScreen : Screen
