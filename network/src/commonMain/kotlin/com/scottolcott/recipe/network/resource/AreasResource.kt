package com.scottolcott.recipe.network.resource

import io.ktor.resources.Resource

@Resource("list.php") internal class AreasResource(val a: String = "list")
