package com.scottolcott.recipe.domain.producer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.scottolcott.recipe.model.Category
import com.scottolcott.recipe.repository.CategoryRepository
import com.slack.circuit.retained.produceRetainedState
import dev.zacsweers.metro.Inject
import org.mobilenativefoundation.store.store5.StoreReadResponse

@Inject
class CategoriesProducer(private val categoryRepository: CategoryRepository) {

  @Composable
  fun produce(retryTrigger: Int): StoreReadResponse<List<Category>> {
    val categories by
      produceRetainedState<StoreReadResponse<List<Category>>>(
        StoreReadResponse.Initial,
        retryTrigger,
      ) {
        categoryRepository.getCategories().collect {
          if (it !is StoreReadResponse.NoNewData) value = it
        }
      }
    return categories
  }
}
