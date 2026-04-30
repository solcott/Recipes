package com.scottolcott.recipe.repository

import co.touchlab.kermit.Logger
import com.scottolcott.recipe.logErrors
import com.scottolcott.recipe.model.Category
import com.scottolcott.recipe.network.api.CategoryApi
import com.scottolcott.recipe.network.dto.CategoryDto
import com.scottolcott.recipe.storage.dao.CategoryDao
import com.scottolcott.recipe.storage.entity.CategoryEntity
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse
import org.mobilenativefoundation.store.store5.Validator

interface CategoryRepository {

  fun getCategories(): Flow<StoreReadResponse<List<Category>>>
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
internal class CategoryRepositoryImpl(
  private val categoryApi: CategoryApi,
  private val categoryDao: CategoryDao,
  private val logger: Logger,
  private val cacheExpiration: Duration = 6.hours,
) : CategoryRepository {

  private val validator =
    Validator.by<List<Category>> { categories ->
      val now = Clock.System.now()
      categories.isNotEmpty() && categories.all { it.lastFetched.plus(cacheExpiration) > now }
    }

  private val store: Store<CategoryKey, List<Category>> =
    StoreBuilder.from(fetcher = createFetcher(), sourceOfTruth = createSourceOfTruth())
      .validator(validator)
      .build()

  private fun createFetcher(): Fetcher<CategoryKey, List<CategoryDto>> {
    return Fetcher.of { key ->
      when (key) {
        CategoryKey.GetCategories -> categoryApi.getCategories().categories
      }
    }
  }

  private fun createSourceOfTruth(): SourceOfTruth<CategoryKey, List<CategoryDto>, List<Category>> {
    return SourceOfTruth.of(
      reader = { key: CategoryKey ->
        when (key) {
          CategoryKey.GetCategories -> categoryDao.getCategories()
        }.map { categories ->
          categories.map { Category(it.id, it.name, it.thumb, it.description, it.lastFetched) }
        }
      },
      writer = { key: CategoryKey, categories: List<CategoryDto> ->
        when (key) {
          CategoryKey.GetCategories -> {
            categoryDao.deleteAllCategories()
            val lastFetched = Clock.System.now()
            categoryDao.insertCategories(
              categories.map {
                CategoryEntity(it.id, it.name, it.thumbnail, it.description, lastFetched)
              }
            )
          }
        }
      },
      delete = { _: CategoryKey -> categoryDao.deleteAllCategories() },
      deleteAll = { categoryDao.deleteAllCategories() },
    )
  }

  override fun getCategories(): Flow<StoreReadResponse<List<Category>>> {
    return store
      .stream(StoreReadRequest.cached(CategoryKey.GetCategories, false))
      .logErrors(logger, "Error loading categories")
  }
}

internal sealed class CategoryKey {
  data object GetCategories : CategoryKey()
}
