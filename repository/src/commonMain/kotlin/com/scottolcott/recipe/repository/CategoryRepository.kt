package com.scottolcott.recipe.repository

import co.touchlab.kermit.Logger
import com.scottolcott.recipe.logErrors
import com.scottolcott.recipe.model.Category
import com.scottolcott.recipe.model.store.CategoriesKey
import com.scottolcott.recipe.network.api.CategoryApi
import com.scottolcott.recipe.network.dto.CategoryDto
import com.scottolcott.recipe.storage.dao.CategoryDao
import com.scottolcott.recipe.storage.datastore.CategoriesFetchHistoryDataStore
import com.scottolcott.recipe.storage.entity.CategoryEntity
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
import org.mobilenativefoundation.store.store5.Validator

interface CategoryRepository {

  fun getCategories(): Flow<StoreReadResponse<List<Category>>>

  fun getCategories(nameFilter: String): Flow<StoreReadResponse<List<Category>>>
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
internal class CategoryRepositoryImpl(
  private val categoryApi: CategoryApi,
  private val categoryDao: CategoryDao,
  private val fetchHistoryDataStore: CategoriesFetchHistoryDataStore,
  private val logger: Logger,
  private val cacheExpiration: Duration = 6.hours,
) : CategoryRepository {

  private val validator =
    Validator.by<List<Category>> { categories ->
      val now = Clock.System.now()
      categories.isNotEmpty() && categories.all { it.lastFetched.plus(cacheExpiration) > now }
    }

  private val converter: Converter<List<CategoryDto>, List<CategoryEntity>, List<Category>> =
    Converter.Builder<List<CategoryDto>, List<CategoryEntity>, List<Category>>()
      .fromNetworkToLocal { dtos ->
        val lastFetched = Clock.System.now()
        dtos.map { CategoryEntity(it.id, it.name, it.thumbnail, it.description, lastFetched) }
      }
      .fromOutputToLocal { dtos ->
        dtos.map { CategoryEntity(it.id, it.name, it.thumb, it.description, it.lastFetched) }
      }
      .build()

  private val store: Store<CategoriesKey, List<Category>> =
    StoreBuilder.from(
        fetcher = createFetcher(),
        sourceOfTruth = createSourceOfTruth(),
        converter = converter,
      )
      .validator(validator)
      .build()

  private fun createFetcher(): Fetcher<CategoriesKey, List<CategoryDto>> {
    return Fetcher.of { key ->
      when (key) {
        CategoriesKey.GetCategories,
        is CategoriesKey.FilterByName -> categoryApi.getCategories().categories
      }
    }
  }

  private fun createSourceOfTruth():
    SourceOfTruth<CategoriesKey, List<CategoryEntity>, List<Category>> {
    return SourceOfTruth.of(
      reader = { key: CategoriesKey ->
        when (key) {
          CategoriesKey.GetCategories -> categoryDao.getCategories()
          is CategoriesKey.FilterByName -> categoryDao.getCategories(key.nameFilter)
        }.map { categories ->
          categories.map { Category(it.id, it.name, it.thumb, it.description, it.lastFetched) }
        }
      },
      writer = { key: CategoriesKey, categories: List<CategoryEntity> ->
        when (key) {
          CategoriesKey.GetCategories,
          is CategoriesKey.FilterByName -> {
            categoryDao.deleteAllCategories()
            val lastFetched = Clock.System.now()
            categoryDao.insertCategories(
              categories.map {
                CategoryEntity(it.id, it.name, it.thumb, it.description, lastFetched)
              }
            )
          }
        }
      },
      delete = { _: CategoriesKey -> categoryDao.deleteAllCategories() },
      deleteAll = { categoryDao.deleteAllCategories() },
    )
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun getCategories(): Flow<StoreReadResponse<List<Category>>> {
    return loadCategoriesByKey(CategoriesKey.GetCategories)
  }

  override fun getCategories(nameFilter: String): Flow<StoreReadResponse<List<Category>>> {
    return loadCategoriesByKey(CategoriesKey.FilterByName(nameFilter))
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  private fun loadCategoriesByKey(key: CategoriesKey): Flow<StoreReadResponse<List<Category>>> {
    return fetchHistoryDataStore
      .refreshNeeded(key, cacheExpiration)
      .flatMapLatest { refresh -> store.stream(StoreReadRequest.cached(key, refresh)) }
      .logErrors(logger, "Error loading categories by $key")
  }
}
