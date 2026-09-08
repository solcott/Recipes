package com.scottolcott.recipe.network.api

import com.scottolcott.recipe.network.ApiClient
import com.scottolcott.recipe.network.dto.AreaResponseDto
import com.scottolcott.recipe.network.resource.AreasResource
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.get

interface AreaApi {
  suspend fun getAreas(): AreaResponseDto
}

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
internal class AreaApiImpl(@ApiClient private val client: HttpClient) : AreaApi {
  override suspend fun getAreas(): AreaResponseDto {
    return client.get(AreasResource()).body()
  }
}
