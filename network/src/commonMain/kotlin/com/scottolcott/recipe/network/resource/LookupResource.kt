package com.scottolcott.recipe.network.resource

import com.scottolcott.recipe.model.RecipeId
import io.ktor.resources.Resource

@Resource("lookup.php") internal class LookupResource(val i: RecipeId)
