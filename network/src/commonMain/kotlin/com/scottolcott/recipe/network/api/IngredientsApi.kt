package com.scottolcott.recipe.network.api

import com.scottolcott.recipe.network.ApiClient
import com.scottolcott.recipe.network.dto.IngredientResponseDto
import com.scottolcott.recipe.network.resource.ListResource
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.get

interface IngredientsApi {
  suspend fun getIngredients(): IngredientResponseDto
}

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
internal class IngredientsApiImpl(@param:ApiClient private val client: HttpClient) :
  IngredientsApi {
  override suspend fun getIngredients(): IngredientResponseDto {
    return client.get(ListResource.IngredientsResource()).body()
  }
}
