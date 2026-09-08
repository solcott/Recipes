package com.scottolcott.recipe.domain.producer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.scottolcott.recipe.model.Ingredient
import com.scottolcott.recipe.repository.IngredientRepository
import com.slack.circuit.retained.produceRetainedState
import dev.zacsweers.metro.Inject
import org.mobilenativefoundation.store.store5.StoreReadResponse

@Inject
internal class IngredientsProducer(private val ingredientsRepository: IngredientRepository) {

  @Composable
  fun produce(retryTrigger: Int): StoreReadResponse<List<Ingredient>> {
    val ingredients by
      produceRetainedState<StoreReadResponse<List<Ingredient>>>(
        StoreReadResponse.Initial,
        retryTrigger,
      ) {
        ingredientsRepository.getIngredients().collect {
          if (it !is StoreReadResponse.NoNewData) value = it
        }
      }
    return ingredients
  }
}
