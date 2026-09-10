package com.scottolcott.recipe.domain.producer

import androidx.compose.runtime.Composable
import com.scottolcott.recipe.model.Area
import com.scottolcott.recipe.repository.AreaRepository
import dev.zacsweers.metro.Inject
import org.mobilenativefoundation.store.store5.StoreReadResponse

@Inject
internal class AreasProducer(private val areasRepository: AreaRepository) {

  @Composable
  fun produce(retryTrigger: Int): StoreReadResponse<List<Area>> =
    produceStoreState(retryTrigger) { areasRepository.getAreas() }
}
