package com.scottolcott.recipe.domain.presenter

import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals

private fun presenterFor(screen: HomeScreen) = HomePresenter(screen, FakeNavigator(screen))

val homePresenterTests by testSuite {
  test("the screen's tab is the initially selected one") {
    presenterFor(HomeScreen(AreasScreen)).test {
      val state = awaitItem()
      assertEquals(AreasScreen, state.selectedTabScreen)
      assertEquals(2, state.selectedIndex)
    }
  }

  test("categories is the default tab") {
    presenterFor(HomeScreen()).test {
      val state = awaitItem()
      assertEquals(CategoriesScreen, state.selectedTabScreen)
      assertEquals(0, state.selectedIndex)
    }
  }

  test("every tab is offered in pager order") {
    presenterFor(HomeScreen()).test {
      assertEquals(
        listOf(CategoriesScreen, IngredientsScreen, AreasScreen),
        awaitItem().tabScreens,
      )
    }
  }

  test("TabSelected moves the selection and its index") {
    presenterFor(HomeScreen()).test {
      awaitItem().eventSink(HomeEvent.TabSelected(IngredientsScreen))
      val state = awaitItem()
      assertEquals(IngredientsScreen, state.selectedTabScreen)
      assertEquals(1, state.selectedIndex)
    }
  }
}
