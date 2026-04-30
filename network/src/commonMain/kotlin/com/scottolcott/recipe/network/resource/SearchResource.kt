package com.scottolcott.recipe.network.resource

import io.ktor.resources.Resource

@Resource("search.php") internal class SearchResource(val s: String)
