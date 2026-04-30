package com.scottolcott.recipe.network.resource

import io.ktor.resources.Resource

@Resource("filter.php")
internal class FilterResource(val c: String? = null, val a: String? = null) {}
