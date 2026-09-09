package com.scottolcott.recipe.repository

import co.touchlab.kermit.Logger
import com.scottolcott.recipe.logErrors
import com.scottolcott.recipe.model.Area
import com.scottolcott.recipe.model.store.AreasKey
import com.scottolcott.recipe.network.api.AreaApi
import com.scottolcott.recipe.network.dto.AreaDto
import com.scottolcott.recipe.storage.dao.AreaDao
import com.scottolcott.recipe.storage.datastore.AreasFetchHistoryDataStore
import com.scottolcott.recipe.storage.entity.AreaEntity
import com.scottolcott.recipe.swapType
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.collections.orEmpty
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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

  fun filterAreasByName(nameFilter: String): Flow<StoreReadResponse<List<Area>>>

  fun getArea(area: String): Flow<StoreReadResponse<Area?>>
}

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

  private val fetcher: Fetcher<AreasKey, List<AreaDto>> = Fetcher.of { key ->
    when (key) {
      AreasKey.GetAll,
      is AreasKey.FilterByName,
      is AreasKey.GetArea -> api.getAreas().meals.orEmpty()
    }
  }

  private val sourceOfTruth: SourceOfTruth<AreasKey, List<AreaEntity>, List<Area>> =
    SourceOfTruth.of(
      reader = { key: AreasKey ->
        when (key) {
          AreasKey.GetAll -> dao.getAllAreasAsFlow().mapToAreas()

          is AreasKey.FilterByName -> dao.filterByName(key.text).mapToAreas()
          is AreasKey.GetArea ->
            dao.getAreaAsFlow(key.area).map {
              if (it == null) {
                emptyList()
              } else {
                listOf(it.toArea())
              }
            }
        }
      },
      writer = { key, local ->
        when (key) {
          AreasKey.GetAll,
          is AreasKey.FilterByName,
          is AreasKey.GetArea -> {
            dao.insert(local)
          }
        }
        val now = Clock.System.now()
        fetchHistoryDataStore.updateLastFetchTime(now)
      },
      delete = { key ->
        when (key) {
          AreasKey.GetAll -> dao.deleteAll()
          is AreasKey.FilterByName -> dao.deleteWhereNameLike(key.text)
          is AreasKey.GetArea -> dao.deleteArea(key.area)
        }
      },
      deleteAll = { dao.deleteAll() },
    )

  private val converter: Converter<List<AreaDto>, List<AreaEntity>, List<Area>> =
    Converter.Builder<List<AreaDto>, List<AreaEntity>, List<Area>>()
      .fromNetworkToLocal { dtos ->
        val lastFetched = Clock.System.now()
        dtos.map { dto -> AreaEntity(dto.area, dto.country, lastFetched) }
      }
      .fromOutputToLocal { models ->
        models.map { AreaEntity(it.area, it.country, it.lastFetched) }
      }
      .build()

  private val store: Store<AreasKey, List<Area>> =
    StoreBuilder.from(fetcher, sourceOfTruth, converter).build()

  override fun getAreas(): Flow<StoreReadResponse<List<Area>>> {
    return loadAreasByKey(AreasKey.GetAll)
  }

  override fun filterAreasByName(nameFilter: String): Flow<StoreReadResponse<List<Area>>> {
    return loadAreasByKey(AreasKey.FilterByName(nameFilter))
  }

  override fun getArea(area: String): Flow<StoreReadResponse<Area?>> {
    return loadAreasByKey(AreasKey.GetArea(area)).map {
      when (it) {
        is StoreReadResponse.Data -> {
          val area = it.value.firstOrNull()
          StoreReadResponse.Data(area, it.origin)
        }
        else -> it.swapType()
      }
    }
  }

  private fun loadAreasByKey(key: AreasKey): Flow<StoreReadResponse<List<Area>>> {
    return fetchHistoryDataStore
      .refreshNeeded(cacheExpiration)
      .flatMapLatest { refresh -> store.stream(StoreReadRequest.cached(key, refresh)) }
      .logErrors(logger, "Error loading areas by $key")
  }

  private fun Flow<List<AreaEntity>>.mapToAreas(): Flow<List<Area>> = map { entities ->
    entities.map { it.toArea() }
  }

  private fun AreaEntity.toArea() =
    Area(
      area = area,
      country = country,
      lastFetched = lastFetched,
    )
}
