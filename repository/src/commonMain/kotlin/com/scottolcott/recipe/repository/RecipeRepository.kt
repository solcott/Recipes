package com.scottolcott.recipe.repository

import co.touchlab.kermit.Logger
import com.scottolcott.recipe.logErrors
import com.scottolcott.recipe.model.Ingredient
import com.scottolcott.recipe.model.Recipe
import com.scottolcott.recipe.model.RecipeDetails
import com.scottolcott.recipe.model.RecipeId
import com.scottolcott.recipe.network.api.RecipeApi
import com.scottolcott.recipe.network.dto.RecipeBasicDto
import com.scottolcott.recipe.network.dto.RecipeDto
import com.scottolcott.recipe.network.dto.RecipeFullDto
import com.scottolcott.recipe.storage.dao.RecipeDao
import com.scottolcott.recipe.storage.datastore.RecipeFavoritesDataStore
import com.scottolcott.recipe.storage.datastore.SearchSearchSuggestionsDataStore
import com.scottolcott.recipe.storage.entity.RecipeDetailEntity
import com.scottolcott.recipe.storage.entity.RecipeEntity
import com.scottolcott.recipe.storage.entity.RecipeEntityWithDetail
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse
import org.mobilenativefoundation.store.store5.StoreReadResponse.Data
import org.mobilenativefoundation.store.store5.StoreReadResponse.Loading
import org.mobilenativefoundation.store.store5.StoreReadResponse.NoNewData
import org.mobilenativefoundation.store.store5.Validator

interface RecipeRepository {
  fun searchRecipes(query: String): Flow<StoreReadResponse<List<Recipe>>>

  fun recipesByCategory(category: String): Flow<StoreReadResponse<List<Recipe>>>

  fun getById(id: RecipeId): Flow<StoreReadResponse<Recipe?>>

  suspend fun addFavorite(id: RecipeId)

  suspend fun removeFavorite(id: RecipeId)

  suspend fun addSearchSuggestion(suggestion: String)

  fun getSearchSuggestionsAsFlow(query: String): Flow<List<String>>
}

internal sealed class RecipeKey {
  data class Query(val query: String) : RecipeKey()

  data class ById(val id: RecipeId) : RecipeKey()

  data class ByCategory(val category: String) : RecipeKey()

  data class ByArea(val area: String) : RecipeKey()
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
internal class RecipeRepositoryImpl(
  private val recipeApi: RecipeApi,
  private val recipeDao: RecipeDao,
  private val favoritesDataStore: RecipeFavoritesDataStore,
  private val suggestionsDataStore: SearchSearchSuggestionsDataStore,
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

  override fun searchRecipes(query: String): Flow<StoreReadResponse<List<Recipe>>> {
    return detailedRecipeStore
      .stream(StoreReadRequest.cached(RecipeKey.Query(query.trim()), false))
      .onStart { addSearchSuggestion(query) }
      .map {
        when (it) {
          is Data<RecipeResponse> -> Data(it.value.recipes, it.origin)
          else -> it.swapType()
        }
      }
      .logErrors(logger, "Error searching recipes by $query")
  }

  override fun recipesByCategory(category: String): Flow<StoreReadResponse<List<Recipe>>> {
    return basicRecipeStore
      .stream(StoreReadRequest.cached(RecipeKey.ByCategory(category), false))
      .map {
        when (it) {
          is Data<RecipeResponse> -> Data(it.value.recipes, it.origin)
          else -> it.swapType()
        }
      }
      .logErrors(logger, "Error loading recipes by category $category")
  }

  override fun getById(id: RecipeId): Flow<StoreReadResponse<Recipe?>> {
    return detailedRecipeStore
      .stream(StoreReadRequest.cached(RecipeKey.ById(id), false))
      .map {
        when (it) {
          is Data<RecipeResponse> -> Data(it.value.recipes.firstOrNull(), it.origin)
          else -> it.swapType()
        }
      }
      .logErrors(logger, "Error loading recipes by id : $id")
  }

  fun getFavoritesIdsAsFlow(): Flow<List<RecipeId>> {
    return favoritesDataStore.favorites.map { it.favorites }
  }

  override suspend fun addFavorite(id: RecipeId) {
    favoritesDataStore.add(id)
  }

  override suspend fun removeFavorite(id: RecipeId) {
    favoritesDataStore.remove(id)
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
        is RecipeKey.ByArea -> recipeApi.getByCategory(key.area)?.meals.orEmpty()
      }
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  private fun createSourceOfTruth(): SourceOfTruth<RecipeKey, List<RecipeDto>, RecipeResponse> {
    return SourceOfTruth.of(
      reader = { key: RecipeKey ->
        combine(
            flow =
              when (key) {
                is RecipeKey.Query -> recipeDao.queryByName(key.query)

                is RecipeKey.ById -> {
                  recipeDao.getById(key.id).map { listOfNotNull(it) }
                }

                is RecipeKey.ByCategory -> recipeDao.getByCategory(key.category)
                is RecipeKey.ByArea -> recipeDao.getByArea(key.area)
              },
            getFavoritesIdsAsFlow(),
          ) { recipes, favorites ->
            RecipeEntitiesAndFavorites(recipes, favorites)
          }
          .map {
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
      },
    )
  }
}

private data class RecipeEntitiesAndFavorites(
  val recipes: List<RecipeEntityWithDetail>,
  val favorites: List<RecipeId>,
)

private fun RecipeDto.toEntity(
  category: String?,
  area: String?,
  lastFetched: Instant,
): RecipeEntityWithDetail {
  return when (this) {
    is RecipeBasicDto ->
      RecipeEntityWithDetail(
        RecipeEntity(
          id = id,
          name = name,
          category = category,
          area = area,
          thumbnail = thumbnail,
          lastFetched = lastFetched,
        ),
        null,
      )
    is RecipeFullDto ->
      RecipeEntityWithDetail(
        RecipeEntity(
          id = id,
          name = name,
          category = this.category,
          area = this.area,
          thumbnail = thumbnail,
          lastFetched = lastFetched,
        ),
        RecipeDetailEntity(
          id = id,
          alternateName = alternateName,
          instructions = instructions,
          tags = tags,
          youtube = youtube,
          source = source,
          imageSource = imageSource,
          creativeCommonsConfirmed = creativeCommonsConfirmed,
          dateModified = dateModified,
          ingredient1 = ingredient1,
          measure1 = measure1,
          ingredient2 = ingredient2,
          measure2 = measure2,
          ingredient3 = ingredient3,
          measure3 = measure3,
          ingredient4 = ingredient4,
          measure4 = measure4,
          ingredient5 = ingredient5,
          measure5 = measure5,
          ingredient6 = ingredient6,
          measure6 = measure6,
          ingredient7 = ingredient7,
          measure7 = measure7,
          ingredient8 = ingredient8,
          measure8 = measure8,
          ingredient9 = ingredient9,
          measure9 = measure9,
          ingredient10 = ingredient10,
          measure10 = measure10,
          ingredient11 = ingredient11,
          measure11 = measure11,
          ingredient12 = ingredient12,
          measure12 = measure12,
          ingredient13 = ingredient13,
          measure13 = measure13,
          ingredient14 = ingredient14,
          measure14 = measure14,
          ingredient15 = ingredient15,
          measure15 = measure15,
          ingredient16 = ingredient16,
          measure16 = measure16,
          ingredient17 = ingredient17,
          measure17 = measure17,
          ingredient18 = ingredient18,
          measure18 = measure18,
          ingredient19 = ingredient19,
          measure19 = measure19,
          ingredient20 = ingredient20,
          measure20 = measure20,
          lastFetched = lastFetched,
        ),
      )
  }
}

private fun List<RecipeDto>.toEntities(
  category: String?,
  area: String?,
): List<RecipeEntityWithDetail> {
  val lastFetched = Clock.System.now()
  return map { it.toEntity(category, area, lastFetched) }
}

private fun RecipeEntityWithDetail.toModel(favorite: Boolean): Recipe {

  val detail = detail
  val ingredientsList =
    with(detail) {
        if (this == null) return@with emptyList()
        listOf(
          ingredient1 to measure1,
          ingredient2 to measure2,
          ingredient3 to measure3,
          ingredient4 to measure4,
          ingredient5 to measure5,
          ingredient6 to measure6,
          ingredient7 to measure7,
          ingredient8 to measure8,
          ingredient9 to measure9,
          ingredient10 to measure10,
          ingredient11 to measure11,
          ingredient12 to measure12,
          ingredient13 to measure13,
          ingredient14 to measure14,
          ingredient15 to measure15,
          ingredient16 to measure16,
          ingredient17 to measure17,
          ingredient18 to measure18,
          ingredient19 to measure19,
          ingredient20 to measure20,
        )
      }
      .mapNotNull { (ingredientName, measureAmount) ->
        // if ingredient name is null or empt filter it out
        if (!ingredientName.isNullOrBlank()) {
          Ingredient(
            ingredient = ingredientName,
            // This assumes measurement can be null as long as the name isn't
            measure = measureAmount ?: "",
          )
        } else {
          null
        }
      }
  return Recipe(
    id = recipe.id,
    name = recipe.name,
    thumbnail = recipe.thumbnail,
    category = recipe.category,
    area = recipe.area,
    favorite = favorite,
    details =
      if (detail == null) {
        null
      } else {
        with(detail) {
          RecipeDetails(
            alternateName = alternateName,
            instructions = instructions,
            tags = tags,
            youtube = youtube,
            source = source,
            imageSource = imageSource,
            creativeCommonsConfirmed = creativeCommonsConfirmed,
            dateModified = dateModified,
            ingredients = ingredientsList,
            lastFetched = lastFetched,
          )
        }
      },
    lastFetched = recipe.lastFetched,
  )
}

private fun RecipeEntitiesAndFavorites.toModel(): List<Recipe> = recipes.map { recipe ->
  recipe.toModel(favorites.contains(recipe.recipe.id))
}

fun <T> StoreReadResponse<*>.swapType(): StoreReadResponse<T> =
  when (this) {
    is StoreReadResponse.Error -> this
    is Loading -> this
    is NoNewData -> this
    is Data -> throw RuntimeException("cannot swap type for StoreResponse.Data")
    is StoreReadResponse.Initial -> this
  }

data class RecipeResponse(val recipes: List<Recipe>, val query: String?, val id: RecipeId? = null)
