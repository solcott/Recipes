package com.scottolcott.recipe.network.api

import com.scottolcott.recipe.model.RecipeId
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

interface RecipeApi {
  suspend fun getRandomRecipe(): RecipeFullResponseDto?

  suspend fun searchRecipe(query: String): RecipeFullResponseDto?

  suspend fun getRecipe(id: RecipeId): RecipeFullResponseDto?

  suspend fun getByCategory(category: String): RecipeBasicResponseDto?

  suspend fun getByArea(area: String): RecipeBasicResponseDto?
}

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
internal class RecipeApiImpl(val client: HttpClient) : RecipeApi {
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

  override suspend fun getByArea(area: String): RecipeBasicResponseDto? {
    return client.get(FilterResource(a = area)).body()
  }
}
