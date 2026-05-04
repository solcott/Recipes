package com.scottolcott.recipe.domain.producer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.scottolcott.recipe.model.Recipe
import com.scottolcott.recipe.repository.RecipeRepository
import com.slack.circuit.retained.produceRetainedState
import dev.zacsweers.metro.Inject
import org.mobilenativefoundation.store.store5.StoreReadResponse

@Inject
class RecipesProducer(private val recipeRepository: RecipeRepository) {

  @Composable
  fun produceBySearchTerm(searchTerm: String, retryTrigger: Int): StoreReadResponse<List<Recipe>> {

    val recipes by
      produceRetainedState<StoreReadResponse<List<Recipe>>>(
        StoreReadResponse.Initial,
        searchTerm,
        retryTrigger,
      ) {
        recipeRepository.searchRecipes(searchTerm).collect { value = it }
      }
    return recipes
  }

  @Composable
  fun produceByCategory(category: String, retryTrigger: Int): StoreReadResponse<List<Recipe>> {
    val recipes by
      produceRetainedState<StoreReadResponse<List<Recipe>>>(
        StoreReadResponse.Initial,
        category,
        retryTrigger,
      ) {
        recipeRepository.recipesByCategory(category).collect { value = it }
      }
    return recipes
  }

  @Composable
  fun produceByFavorites(retryTrigger: Int): StoreReadResponse<List<Recipe>> {
    val recipes by
      produceRetainedState<StoreReadResponse<List<Recipe>>>(
        StoreReadResponse.Initial,
        retryTrigger,
      ) {
        recipeRepository.getFavoritesAsFlow().collect { value = it }
      }
    return recipes
  }
}
