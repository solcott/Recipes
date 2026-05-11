package com.scottolcott.recipe.repository

import co.touchlab.kermit.Logger
import com.scottolcott.recipe.logErrors
import com.scottolcott.recipe.model.Recipe
import com.scottolcott.recipe.model.RecipeId
import com.scottolcott.recipe.model.RecipeKey
import com.scottolcott.recipe.network.api.RecipeApi
import com.scottolcott.recipe.network.dto.RecipeDto
import com.scottolcott.recipe.storage.dao.RecipeDao
import com.scottolcott.recipe.storage.datastore.RecipeFetchHistoryDataStore
import com.scottolcott.recipe.storage.datastore.SearchSearchSuggestionsDataStore
import com.scottolcott.recipe.storage.entity.FavoriteEntity
import com.scottolcott.recipe.swapType
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse
import org.mobilenativefoundation.store.store5.StoreReadResponse.Data
import org.mobilenativefoundation.store.store5.Validator

interface RecipeRepository {
  fun searchRecipes(query: String): Flow<StoreReadResponse<List<Recipe>>>

  fun recipesByCategory(category: String): Flow<StoreReadResponse<List<Recipe>>>

  fun recipesByArea(area: String): Flow<StoreReadResponse<List<Recipe>>>

  fun getById(id: RecipeId): Flow<StoreReadResponse<Recipe?>>

  fun getFavoritesAsFlow(): Flow<StoreReadResponse<List<Recipe>>>

  suspend fun addFavorite(id: RecipeId)

  suspend fun removeFavorite(id: RecipeId)

  suspend fun addSearchSuggestion(suggestion: String)

  fun getSearchSuggestionsAsFlow(query: String): Flow<List<String>>
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
internal class RecipeRepositoryImpl(
  private val recipeApi: RecipeApi,
  private val recipeDao: RecipeDao,
  private val suggestionsDataStore: SearchSearchSuggestionsDataStore,
  private val fetchHistoryDataStore: RecipeFetchHistoryDataStore,
  private val logger: Logger,
  private val cacheExpiration: Duration = 1.hours,
) : RecipeRepository {
  private val storeBuilder: StoreBuilder<RecipeKey, RecipeResponse> =
    StoreBuilder.from(createFetcher(), createSourceOfTruth())

  val detailedRecipeValidator =
    Validator.by<RecipeResponse> { item ->
      val now = Clock.System.now()
      item.recipes.isNotEmpty() &&
        item.recipes.all {
          val lastFetched = it.details?.lastFetched
          it.lastFetched.plus(cacheExpiration) > now &&
            lastFetched != null &&
            lastFetched.plus(cacheExpiration) > now
        }
    }

  /** This store is for api methods that return full recipe details */
  private val detailedRecipeStore: Store<RecipeKey, RecipeResponse> =
    storeBuilder.validator(detailedRecipeValidator).build()

  val basicRecipeValidator =
    Validator.by<RecipeResponse> { item ->
      val now = Clock.System.now()
      item.recipes.isNotEmpty() && item.recipes.all { it.lastFetched.plus(cacheExpiration) > now }
    }

  /** This store is for api methods that only return basic recipe details */
  private val basicRecipeStore: Store<RecipeKey, RecipeResponse> =
    storeBuilder.validator(basicRecipeValidator).build()

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun searchRecipes(query: String): Flow<StoreReadResponse<List<Recipe>>> {
    val key = RecipeKey.Query(query.trim())
    return refreshNeeded(key)
      .flatMapLatest { refresh ->
        detailedRecipeStore.stream(StoreReadRequest.cached(key, refresh))
      }
      .onStart { addSearchSuggestion(query) }
      .map {
        when (it) {
          is Data<RecipeResponse> -> Data(it.value.recipes, it.origin)
          else -> it.swapType()
        }
      }
      .logErrors(logger, "Error searching recipes by $query")
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun recipesByCategory(category: String): Flow<StoreReadResponse<List<Recipe>>> {
    val key = RecipeKey.ByCategory(category)
    return refreshNeeded(key)
      .flatMapLatest { refresh -> basicRecipeStore.stream(StoreReadRequest.cached(key, refresh)) }
      .map {
        when (it) {
          is Data<RecipeResponse> -> Data(it.value.recipes, it.origin)
          else -> it.swapType()
        }
      }
      .logErrors(logger, "Error loading recipes by category $category")
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun recipesByArea(area: String): Flow<StoreReadResponse<List<Recipe>>> {
    val key = RecipeKey.ByArea(area)
    return refreshNeeded(key)
      .flatMapLatest { refresh -> basicRecipeStore.stream(StoreReadRequest.cached(key, refresh)) }
      .map {
        when (it) {
          is Data<RecipeResponse> -> Data(it.value.recipes, it.origin)
          else -> it.swapType()
        }
      }
      .logErrors(logger, "Error loading recipes by area $area")
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun getById(id: RecipeId): Flow<StoreReadResponse<Recipe?>> {
    val key = RecipeKey.ById(id)
    return refreshNeeded(key)
      .flatMapLatest { refresh ->
        detailedRecipeStore.stream(StoreReadRequest.cached(key, refresh))
      }
      .map {
        when (it) {
          is Data<RecipeResponse> -> Data(it.value.recipes.firstOrNull(), it.origin)
          else -> it.swapType()
        }
      }
      .logErrors(logger, "Error loading recipes by id : $id")
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun getFavoritesAsFlow(): Flow<StoreReadResponse<List<Recipe>>> {
    return detailedRecipeStore
      .stream(StoreReadRequest.cached(RecipeKey.Favorites, false))
      .map {
        when (it) {
          is Data<RecipeResponse> -> Data(it.value.recipes, it.origin)
          else -> it.swapType()
        }
      }
      .logErrors(logger, "Error getting recipe favorites")
  }

  override suspend fun addFavorite(id: RecipeId) {
    recipeDao.insert(FavoriteEntity(id, Clock.System.now()))
  }

  override suspend fun removeFavorite(id: RecipeId) {
    recipeDao.deleteFavorite(id)
  }

  override suspend fun addSearchSuggestion(suggestion: String) {
    suggestionsDataStore.add(suggestion.trim())
  }

  override fun getSearchSuggestionsAsFlow(query: String): Flow<List<String>> {
    return suggestionsDataStore.suggestions.map { searchSuggestions ->
      searchSuggestions.suggestions
        .filter { suggestion -> suggestion.startsWith(query.trim()) }
        .sortedWith(String.CASE_INSENSITIVE_ORDER)
    }
  }

  private fun createFetcher(): Fetcher<RecipeKey, List<RecipeDto>> {
    return Fetcher.of { key ->
      when (key) {
        is RecipeKey.Query -> recipeApi.searchRecipe(key.query)?.meals.orEmpty()
        is RecipeKey.ById -> recipeApi.getRecipe(key.id)?.meals.orEmpty()
        is RecipeKey.ByCategory -> recipeApi.getByCategory(key.category)?.meals.orEmpty()
        is RecipeKey.ByArea -> recipeApi.getByArea(key.area)?.meals.orEmpty()
        RecipeKey.Favorites -> emptyList() // No api to support this as favorites are store locally
      }
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  private fun createSourceOfTruth(): SourceOfTruth<RecipeKey, List<RecipeDto>, RecipeResponse> {
    return SourceOfTruth.of(
      reader = { key: RecipeKey ->
        when (key) {
          is RecipeKey.Query -> recipeDao.queryByName(key.query)

          is RecipeKey.ById -> {
            recipeDao.getById(key.id).map { listOfNotNull(it) }
          }

          is RecipeKey.ByCategory -> recipeDao.getByCategory(key.category)
          is RecipeKey.ByArea -> recipeDao.getByArea(key.area)
          is RecipeKey.Favorites -> recipeDao.getFavorites()
        }.map {
          RecipeResponse(
            it.toModel(),
            query =
              when (key) {
                is RecipeKey.Query -> key.query
                else -> null
              },
          )
        }
      },
      writer = { key, dtos ->
        val area =
          when (key) {
            is RecipeKey.ByArea -> key.area
            else -> null
          }
        val category =
          when (key) {
            is RecipeKey.ByCategory -> key.category
            else -> null
          }

        recipeDao.insert(dtos.toEntities(category, area))
        fetchHistoryDataStore.updateLastFetchTime(key, Clock.System.now())
      },
    )
  }

  private fun refreshNeeded(key: RecipeKey): Flow<Boolean> {
    return fetchHistoryDataStore.history.map { history ->
      val lastFetch = history.lastFetchTimes[key]
      lastFetch == null || lastFetch.plus(cacheExpiration) < Clock.System.now()
    }
  }
}

data class RecipeResponse(val recipes: List<Recipe>, val query: String?, val id: RecipeId? = null)
