package com.scottolcott.recipe.repository

import co.touchlab.kermit.Logger
import com.scottolcott.recipe.logErrors
import com.scottolcott.recipe.model.Ingredient
import com.scottolcott.recipe.model.store.IngredientsKey
import com.scottolcott.recipe.network.api.IngredientsApi
import com.scottolcott.recipe.network.dto.IngredientDto
import com.scottolcott.recipe.storage.dao.IngredientDao
import com.scottolcott.recipe.storage.datastore.IngredientsFetchHistoryDataStore
import com.scottolcott.recipe.storage.entity.IngredientEntity
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
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

interface IngredientRepository {

  fun getIngredients(): Flow<StoreReadResponse<List<Ingredient>>>

  fun filterIngredientsByName(nameFilter: String): Flow<StoreReadResponse<List<Ingredient>>>
}

// detekt 2.0.0-alpha.6 false positive: UnusedPrivateProperty misses references made from lambdas
// in property initializers, i.e. `fetcher` and `sourceOfTruth` below. CategoryRepositoryImpl
// builds the same objects in member functions instead and is not flagged. Remove on detekt upgrade.
@Suppress("UnusedPrivateProperty")
@OptIn(ExperimentalCoroutinesApi::class)
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
internal class IngredientRepositoryImpl(
  private val api: IngredientsApi,
  private val dao: IngredientDao,
  private val fetchHistoryDataStore: IngredientsFetchHistoryDataStore,
  private val logger: Logger,
  private val cacheExpiration: Duration = 6.hours,
) : IngredientRepository {

  private val fetcher: Fetcher<IngredientsKey, List<IngredientDto>> = Fetcher.of { key ->
    when (key) {
      IngredientsKey.GetAll,
      is IngredientsKey.FilterByName -> api.getIngredients().meals.orEmpty()
    }
  }

  private val sourceOfTruth:
    SourceOfTruth<IngredientsKey, List<IngredientEntity>, List<Ingredient>> =
    SourceOfTruth.of(
      reader = { key: IngredientsKey ->
        when (key) {
          IngredientsKey.GetAll -> dao.getAllIngredientsAsFlow().mapToIngredients()

          is IngredientsKey.FilterByName -> dao.filterByName(key.text).mapToIngredients()
        }
      },
      writer = { key, local ->
        when (key) {
          IngredientsKey.GetAll,
          is IngredientsKey.FilterByName -> {
            dao.insert(local)
          }
        }
        val now = Clock.System.now()
        fetchHistoryDataStore.updateLastFetchTime(key, now, now.minus(cacheExpiration))
      },
      delete = { key ->
        when (key) {
          IngredientsKey.GetAll -> dao.deleteAll()
          is IngredientsKey.FilterByName -> dao.deleteWhereNameLike(key.text)
        }
      },
      deleteAll = { dao.deleteAll() },
    )

  private val converter: Converter<List<IngredientDto>, List<IngredientEntity>, List<Ingredient>> =
    Converter.Builder<List<IngredientDto>, List<IngredientEntity>, List<Ingredient>>()
      .fromNetworkToLocal { dtos ->
        val lastFetched = Clock.System.now()
        dtos.map { dto ->
          IngredientEntity(dto.id, dto.name, dto.description, dto.type, dto.thumbnail, lastFetched)
        }
      }
      .fromOutputToLocal { models ->
        models.map {
          IngredientEntity(it.id, it.name, it.description, it.type, it.thumbnail, it.lastFetched)
        }
      }
      .build()

  private val store: Store<IngredientsKey, List<Ingredient>> =
    StoreBuilder.from(fetcher, sourceOfTruth, converter).build()

  override fun getIngredients(): Flow<StoreReadResponse<List<Ingredient>>> {
    return loadIngredientsByKey(IngredientsKey.GetAll)
  }

  override fun filterIngredientsByName(
    nameFilter: String
  ): Flow<StoreReadResponse<List<Ingredient>>> {
    return loadIngredientsByKey(IngredientsKey.FilterByName(nameFilter))
  }

  private fun loadIngredientsByKey(key: IngredientsKey): Flow<StoreReadResponse<List<Ingredient>>> {
    return fetchHistoryDataStore
      .refreshNeeded(key, cacheExpiration)
      .flatMapLatest { refresh -> store.stream(StoreReadRequest.cached(key, refresh)) }
      .logErrors(logger, "Error loading ingredients by $key")
  }

  private fun Flow<List<IngredientEntity>>.mapToIngredients(): Flow<List<Ingredient>> =
    map { entities ->
      entities.map { it.toIngredient() }
    }

  private fun IngredientEntity.toIngredient() =
    Ingredient(
      id = id,
      name = name,
      description = description,
      type = type,
      thumbnail = thumbnail,
      lastFetched = lastFetched,
    )
}
