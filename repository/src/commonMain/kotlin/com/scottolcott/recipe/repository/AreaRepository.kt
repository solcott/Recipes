package com.scottolcott.recipe.repository

import co.touchlab.kermit.Logger
import com.scottolcott.recipe.logErrors
import com.scottolcott.recipe.model.Area
import com.scottolcott.recipe.network.api.AreaApi
import com.scottolcott.recipe.network.dto.AreaDto
import com.scottolcott.recipe.storage.dao.AreaDao
import com.scottolcott.recipe.storage.datastore.AreasFetchHistoryDataStore
import com.scottolcott.recipe.storage.entity.AreaEntity
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import org.mobilenativefoundation.store.store5.Converter
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse

interface AreaRepository {

  fun getAreas(): Flow<StoreReadResponse<List<Area>>>

  /**
   * The country [area] names, or `null` if the list does not know this area or could not be loaded.
   *
   * A one-shot rather than a [Flow] on purpose: [RecipeRepository] needs the country to build one
   * network request, and driving a Store key off a flow that emits before the areas cache is warm
   * makes the key change mid-load and refetches everything behind it.
   */
  suspend fun countryFor(area: String): String?
}

// detekt 2.0.0-alpha.6 false positive: UnusedPrivateProperty misses references made from lambdas
// in property initializers, i.e. `fetcher` and `sourceOfTruth` below. CategoryRepositoryImpl
// builds the same objects in member functions instead and is not flagged. Remove on detekt upgrade.
@Suppress("UnusedPrivateProperty")
@OptIn(ExperimentalCoroutinesApi::class)
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
internal class AreaRepositoryImpl(
  private val api: AreaApi,
  private val dao: AreaDao,
  private val fetchHistoryDataStore: AreasFetchHistoryDataStore,
  private val logger: Logger,
  private val cacheExpiration: Duration = 6.hours,
) : AreaRepository {

  // `list.php?a=list` has no parameters beyond the literal `list`, so there is nothing to key on:
  // every read is the whole list, and Unit says so rather than a sealed type with one member.
  private val fetcher: Fetcher<Unit, List<AreaDto>> = Fetcher.of { api.getAreas().meals.orEmpty() }

  private val sourceOfTruth: SourceOfTruth<Unit, List<AreaEntity>, List<Area>> =
    SourceOfTruth.of(
      reader = { dao.getAllAreasAsFlow().map { entities -> entities.map { it.toArea() } } },
      writer = { _, local ->
        dao.insert(local)
        fetchHistoryDataStore.updateLastFetchTime(Clock.System.now())
      },
      delete = { dao.deleteAll() },
      deleteAll = { dao.deleteAll() },
    )

  private val converter: Converter<List<AreaDto>, List<AreaEntity>, List<Area>> =
    Converter.Builder<List<AreaDto>, List<AreaEntity>, List<Area>>()
      .fromNetworkToLocal { dtos ->
        val lastFetched = Clock.System.now()
        dtos.map { AreaEntity(it.area, it.country, lastFetched) }
      }
      .fromOutputToLocal { models ->
        models.map { AreaEntity(it.area, it.country, it.lastFetched) }
      }
      .build()

  private val store: Store<Unit, List<Area>> =
    StoreBuilder.from(fetcher, sourceOfTruth, converter).build()

  override fun getAreas(): Flow<StoreReadResponse<List<Area>>> {
    return fetchHistoryDataStore
      .refreshNeeded(cacheExpiration)
      .flatMapLatest { refresh -> store.stream(StoreReadRequest.cached(Unit, refresh)) }
      .logErrors(logger, "Error loading areas")
  }

  override suspend fun countryFor(area: String): String? {
    // Waits for the first *settled* response. A Store stream never completes, so taking `first()`
    // of the data alone would hang forever on an area the list does not contain.
    val settled = getAreas().first { it is StoreReadResponse.Data || it is StoreReadResponse.Error }
    return (settled as? StoreReadResponse.Data)
      ?.value
      ?.firstOrNull { it.area.equals(area, ignoreCase = true) }
      ?.country
  }

  private fun AreaEntity.toArea() = Area(area = area, country = country, lastFetched = lastFetched)
}
