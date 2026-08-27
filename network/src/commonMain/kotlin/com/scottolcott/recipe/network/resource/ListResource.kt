package com.scottolcott.recipe.network.resource

import io.ktor.resources.Resource

@Resource("filter.php")
internal class ListResource {

  data class IngredientsResource(val i: String = "list", val parent: ListResource = ListResource())
}
