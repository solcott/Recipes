package com.scottolcott.recipe.domain.producer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.scottolcott.recipe.model.Area
import com.scottolcott.recipe.repository.AreaRepository
import com.slack.circuit.retained.produceRetainedState
import dev.zacsweers.metro.Inject
import org.mobilenativefoundation.store.store5.StoreReadResponse

@Inject
internal class AreasProducer(private val areasRepository: AreaRepository) {

  @Composable
  fun produce(retryTrigger: Int): StoreReadResponse<List<Area>> {
    val ingredients by
      produceRetainedState<StoreReadResponse<List<Area>>>(
        StoreReadResponse.Initial,
        retryTrigger,
      ) {
        areasRepository.getAreas().collect { if (it !is StoreReadResponse.NoNewData) value = it }
      }
    return ingredients
  }
}
