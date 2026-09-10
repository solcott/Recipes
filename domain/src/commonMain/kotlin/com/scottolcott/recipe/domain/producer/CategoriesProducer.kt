package com.scottolcott.recipe.domain.producer

import androidx.compose.runtime.Composable
import com.scottolcott.recipe.model.Category
import com.scottolcott.recipe.repository.CategoryRepository
import dev.zacsweers.metro.Inject
import org.mobilenativefoundation.store.store5.StoreReadResponse

@Inject
internal class CategoriesProducer(private val categoryRepository: CategoryRepository) {

  @Composable
  fun produce(retryTrigger: Int): StoreReadResponse<List<Category>> =
    produceStoreState(retryTrigger) { categoryRepository.getCategories() }
}
