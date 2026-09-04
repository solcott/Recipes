@file:Suppress("MatchingDeclarationName")

package com.scottolcott.recipe.domain.presenter

import com.slack.circuit.serialization.CircuitSerializable
import dev.zacsweers.metro.AppScope

@CircuitSerializable(AppScope::class)
data object IngredientsScreen : HomeTabScreen {
  override val urlSegment: String = "ingredients"
}
