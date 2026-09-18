package com.scottolcott.recipe.domain.producer

import androidx.compose.runtime.Composable
import com.scottolcott.recipe.model.Ingredient
import com.scottolcott.recipe.repository.IngredientRepository
import dev.zacsweers.metro.Inject
import io.github.solcott.uistate.ContentState
import io.github.solcott.uistate.circuit.produceRetainedContentState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Inject
internal class IngredientsProducer(private val ingredientsRepository: IngredientRepository) {

  @Composable
  fun produce(retryTrigger: Int): ContentState<ImmutableList<Ingredient>> =
    produceRetainedContentState(persistentListOf(), retryTrigger) {
      ingredientsRepository.getIngredients()
    }
}
