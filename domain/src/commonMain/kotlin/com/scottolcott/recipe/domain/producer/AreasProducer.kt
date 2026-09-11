package com.scottolcott.recipe.domain.producer

import androidx.compose.runtime.Composable
import com.scottolcott.recipe.model.Area
import com.scottolcott.recipe.repository.AreaRepository
import dev.zacsweers.metro.Inject
import io.github.solcott.uistate.ContentState
import io.github.solcott.uistate.circuit.produceRetainedContentState

@Inject
internal class AreasProducer(private val areasRepository: AreaRepository) {

  @Composable
  fun produce(retryTrigger: Int): ContentState<List<Area>> =
    produceRetainedContentState(emptyList(), retryTrigger) { areasRepository.getAreas() }
}
