package com.scottolcott.recipe.network.api

import com.scottolcott.recipe.model.RecipeId
import com.scottolcott.recipe.network.ApiClient
import com.scottolcott.recipe.network.dto.RecipeBasicResponseDto
import com.scottolcott.recipe.network.dto.RecipeFullResponseDto
import com.scottolcott.recipe.network.resource.FilterResource
import com.scottolcott.recipe.network.resource.LookupResource
import com.scottolcott.recipe.network.resource.RandomResource
import com.scottolcott.recipe.network.resource.SearchResource
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.get
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

interface RecipeApi {
  suspend fun getRandomRecipe(): RecipeFullResponseDto?

  suspend fun searchRecipe(query: String): RecipeFullResponseDto?

  suspend fun getRecipe(id: RecipeId): RecipeFullResponseDto?

  suspend fun getByCategory(category: String): RecipeBasicResponseDto?

  suspend fun getByIngredient(ingredients: Collection<String>): RecipeBasicResponseDto?

  suspend fun getByArea(area: String, country: String?): RecipeBasicResponseDto
}

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
internal class RecipeApiImpl(@param:ApiClient val client: HttpClient) : RecipeApi {
  override suspend fun getRandomRecipe(): RecipeFullResponseDto? {
    return client.get(RandomResource()).body()
  }

  override suspend fun searchRecipe(query: String): RecipeFullResponseDto? {
    return client.get(SearchResource(query)).body()
  }

  override suspend fun getRecipe(id: RecipeId): RecipeFullResponseDto? {
    return client.get(LookupResource(id)).body()
  }

  override suspend fun getByCategory(category: String): RecipeBasicResponseDto? {
    return client.get(FilterResource(c = category)).body()
  }

  override suspend fun getByIngredient(ingredients: Collection<String>): RecipeBasicResponseDto? {
    return client.get(FilterResource(i = ingredients.joinToString(","))).body()
  }

  /**
   * `filter.php` matches an exact area string, and TheMealDB spells the same cuisine both ways --
   * meals are tagged `Italian` while the area list also carries `Italy` -- so both spellings are
   * asked for and the results merged.
   *
   * The country lookup is best-effort: the shared client sets `expectSuccess`, and a 404 on the
   * second request must not discard a first request that succeeded.
   */
  override suspend fun getByArea(area: String, country: String?): RecipeBasicResponseDto =
    coroutineScope {
      val byArea = async { client.get(FilterResource(a = area)).body<RecipeBasicResponseDto>() }
      val byCountry =
        country
          ?.takeUnless { it.equals(area, ignoreCase = true) }
          ?.let {
            async {
              runCatching { client.get(FilterResource(a = it)).body<RecipeBasicResponseDto>() }
                .getOrNull()
            }
          }
      val all = byArea.await().meals.orEmpty() + byCountry?.await()?.meals.orEmpty()
      RecipeBasicResponseDto(all.distinctBy { it.id })
    }
}
