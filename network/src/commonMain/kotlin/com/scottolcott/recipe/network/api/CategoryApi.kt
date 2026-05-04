package com.scottolcott.recipe.network.api

import com.scottolcott.recipe.network.ApiClient
import com.scottolcott.recipe.network.dto.CategoryResponseDto
import com.scottolcott.recipe.network.resource.CategoryResource
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.get

interface CategoryApi {

  suspend fun getCategories(): CategoryResponseDto
}

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
internal class CategoryApiImpl(@param:ApiClient private val client: HttpClient) : CategoryApi {
  override suspend fun getCategories(): CategoryResponseDto {
    return client.get(CategoryResource()).body()
  }
}
