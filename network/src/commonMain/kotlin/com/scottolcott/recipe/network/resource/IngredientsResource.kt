package com.scottolcott.recipe.network.resource

import io.ktor.resources.Resource

@Resource("list.php") internal class IngredientsResource(val i: String = "list")
