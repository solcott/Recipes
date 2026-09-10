package com.scottolcott.recipe.domain.producer

import androidx.compose.runtime.Composable
import com.scottolcott.recipe.model.Ingredient
import com.scottolcott.recipe.repository.IngredientRepository
import dev.zacsweers.metro.Inject
import org.mobilenativefoundation.store.store5.StoreReadResponse

@Inject
internal class IngredientsProducer(private val ingredientsRepository: IngredientRepository) {

  @Composable
  fun produce(retryTrigger: Int): StoreReadResponse<List<Ingredient>> =
    produceStoreState(retryTrigger) { ingredientsRepository.getIngredients() }
}
