package com.scottolcott.recipe.repository

import co.touchlab.kermit.Logger
import com.scottolcott.recipe.logErrors
import com.scottolcott.recipe.model.Recipe
import com.scottolcott.recipe.model.RecipeId
import com.scottolcott.recipe.model.store.RecipesKey
import com.scottolcott.recipe.network.api.RecipeApi
import com.scottolcott.recipe.network.dto.RecipeDto
import com.scottolcott.recipe.network.dto.RecipeFullDto
import com.scottolcott.recipe.storage.dao.RecipeDao
import com.scottolcott.recipe.storage.datastore.RecipeFetchHistoryDataStore
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse
import org.mobilenativefoundation.store.store5.StoreReadResponse.Data

interface RecipeRepository {
  fun searchRecipes(query: String): Flow<StoreReadResponse<List<Recipe>>>

  fun recipesByCategory(category: String): Flow<StoreReadResponse<List<Recipe>>>

  fun recipesByIngredients(ingredients: Set<String>): Flow<StoreReadResponse<List<Recipe>>>

  fun recipesByArea(area: String): Flow<StoreReadResponse<List<Recipe>>>

  fun getById(id: RecipeId): Flow<StoreReadResponse<Recipe?>>

  fun getFavoritesAsFlow(): Flow<StoreReadResponse<List<Recipe>>>

  suspend fun addFavorite(id: RecipeId)

  suspend fun removeFavorite(id: RecipeId)
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
internal class RecipeRepositoryImpl(
  private val recipeApi: RecipeApi,
  private val recipeDao: RecipeDao,
  private val fetchHistoryDataStore: RecipeFetchHistoryDataStore,
  private val logger: Logger,
  private val cacheExpiration: Duration = 1.hours,
) : RecipeRepository {

  private val recipeStore: Store<RecipesKey, RecipeResponse> =
    StoreBuilder.from(createFetcher(), createSourceOfTruth()).build()

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun searchRecipes(query: String): Flow<StoreReadResponse<List<Recipe>>> {
    val key = RecipesKey.Query(query.trim())
    return fetchHistoryDataStore
      .refreshNeeded(key, cacheExpiration)
      .flatMapLatest { refresh -> recipeStore.stream(StoreReadRequest.cached(key, refresh)) }
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
    val key = RecipesKey.ByCategory(category)
    return fetchHistoryDataStore
      .refreshNeeded(key, cacheExpiration)
      .flatMapLatest { refresh -> recipeStore.stream(StoreReadRequest.cached(key, refresh)) }
      .map {
        when (it) {
          is Data<RecipeResponse> -> Data(it.value.recipes, it.origin)
          else -> it.swapType()
        }
      }
      .logErrors(logger, "Error loading recipes by category $category")
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun recipesByIngredients(
    ingredients: Set<String>
  ): Flow<StoreReadResponse<List<Recipe>>> {
    val key = RecipesKey.ByIngredient.of(ingredients)
    return fetchHistoryDataStore
      .refreshNeeded(key, cacheExpiration)
      .flatMapLatest { refresh -> recipeStore.stream(StoreReadRequest.cached(key, refresh)) }
      .map {
        when (it) {
          is Data<RecipeResponse> -> Data(it.value.recipes, it.origin)
          else -> it.swapType()
        }
      }
      .logErrors(logger, "Error loading recipes by ingredients $ingredients")
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun recipesByArea(area: String): Flow<StoreReadResponse<List<Recipe>>> {
    val key = RecipesKey.ByArea(area)
    return fetchHistoryDataStore
      .refreshNeeded(key, cacheExpiration)
      .flatMapLatest { refresh -> recipeStore.stream(StoreReadRequest.cached(key, refresh)) }
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
    val key = RecipesKey.ById(id)
    return fetchHistoryDataStore
      .refreshNeeded(key, cacheExpiration)
      .flatMapLatest { refresh -> recipeStore.stream(StoreReadRequest.cached(key, refresh)) }
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
    return recipeStore
      .stream(StoreReadRequest.cached(RecipesKey.Favorites, false))
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

  /**
   * `filter.php` only returns summaries, so an ingredient filter is a two-step call: filter for
   * candidate ids, then look each one up for the real ingredient list. That hydration is what
   * populates the `recipe_ingredient` index, which in turn is what makes later filters — including
   * combinations never sent to the API — answerable from the database alone.
   *
   * Ids whose details are already cached and still fresh are skipped, so a filter overlapping
   * recipes already seen costs far fewer requests than the first one did.
   *
   * A lookup that fails is logged and dropped rather than failing the whole filter — the client
   * sets `expectSuccess`, so one bad id would otherwise sink the other 39. Every lookup failing
   * still throws: an empty return is written back as a successful fetch and would suppress the
   * retry for a full [cacheExpiration].
   */
  private suspend fun fetchByIngredients(key: RecipesKey.ByIngredient): List<RecipeFullDto> {
    val queried = key.ingredients.take(MAX_FILTER_INGREDIENTS)
    val summaries = recipeApi.getByIngredient(queried)?.meals.orEmpty()
    val ids = summaries.take(MAX_HYDRATED_RESULTS).map { it.id }
    if (ids.isEmpty()) return emptyList()

    val alreadyFresh =
      recipeDao.idsWithFreshDetail(ids, Clock.System.now().minus(cacheExpiration)).toSet()
    val stale = ids.filterNot { it in alreadyFresh }

    return stale.mapConcurrentlyCatching(
      concurrency = HYDRATION_CONCURRENCY,
      onFailure = { id, error ->
        logger.w(error) { "Skipping recipe $id: hydration lookup failed" }
      },
    ) { id ->
      recipeApi.getRecipe(id)?.meals?.firstOrNull()
    }
  }

  private fun createFetcher(): Fetcher<RecipesKey, List<RecipeDto>> {
    return Fetcher.of { key ->
      when (key) {
        is RecipesKey.Query -> recipeApi.searchRecipe(key.query)?.meals.orEmpty()
        is RecipesKey.ById -> recipeApi.getRecipe(key.id)?.meals.orEmpty()
        is RecipesKey.ByCategory -> recipeApi.getByCategory(key.category)?.meals.orEmpty()
        is RecipesKey.ByArea -> recipeApi.getByArea(key.area)?.meals.orEmpty()
        RecipesKey.Favorites -> emptyList() // No api to support this as favorites are store locally
        is RecipesKey.ByIngredient -> fetchByIngredients(key)
      }
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  private fun createSourceOfTruth(): SourceOfTruth<RecipesKey, List<RecipeDto>, RecipeResponse> {
    return SourceOfTruth.of(
      reader = { key: RecipesKey ->
        when (key) {
          is RecipesKey.Query -> recipeDao.queryByName(key.query)

          is RecipesKey.ById -> {
            recipeDao.getById(key.id).map { listOfNotNull(it) }
          }

          is RecipesKey.ByCategory -> recipeDao.getByCategory(key.category)
          is RecipesKey.ByArea -> recipeDao.getByArea(key.area)
          is RecipesKey.Favorites -> recipeDao.getFavorites()
          is RecipesKey.ByIngredient ->
            recipeDao.getByIngredients(key.ingredients, key.ingredients.size)
        }.map { RecipeResponse(it.toModel()) }
      },
      writer = { key, dtos ->
        val area =
          when (key) {
            is RecipesKey.ByArea -> key.area
            else -> null
          }
        val category =
          when (key) {
            is RecipesKey.ByCategory -> key.category
            else -> null
          }

        recipeDao.insert(dtos.toEntities(category, area))
        val now = Clock.System.now()
        fetchHistoryDataStore.updateLastFetchTime(key, now, now.minus(cacheExpiration))
      },
    )
  }
}

/** `filter.php?i=` accepts at most four comma-separated ingredients on v2. */
private const val MAX_FILTER_INGREDIENTS = 4

/**
 * Cap on lookups per fetch, so a broad filter doesn't fan out into a request per result. The
 * `HAVING` clause in [com.scottolcott.recipe.storage.dao.RecipeDao.getByIngredients] still narrows
 * correctly; it just sees fewer candidates on the first pass.
 */
private const val MAX_HYDRATED_RESULTS = 40

private const val HYDRATION_CONCURRENCY = 4

/**
 * What the source of truth hands back for a [RecipesKey].
 *
 * A wrapper around the list rather than the list itself because Store needs a single output type
 * for every key, including the ones that read a single recipe.
 */
data class RecipeResponse(val recipes: List<Recipe>)
