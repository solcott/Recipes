package com.scottolcott.recipe.domain.producer

import androidx.compose.runtime.Composable
import com.scottolcott.recipe.model.Category
import com.scottolcott.recipe.repository.CategoryRepository
import dev.zacsweers.metro.Inject
import io.github.solcott.uistate.ContentState
import io.github.solcott.uistate.circuit.produceRetainedContentState

@Inject
internal class CategoriesProducer(private val categoryRepository: CategoryRepository) {

  @Composable
  fun produce(retryTrigger: Int): ContentState<List<Category>> =
    produceRetainedContentState(emptyList(), retryTrigger) { categoryRepository.getCategories() }
}
