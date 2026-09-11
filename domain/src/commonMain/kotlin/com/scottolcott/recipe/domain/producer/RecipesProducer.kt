package com.scottolcott.recipe.domain.producer

import androidx.compose.runtime.Composable
import com.scottolcott.recipe.model.Recipe
import com.scottolcott.recipe.repository.RecipeRepository
import dev.zacsweers.metro.Inject
import io.github.solcott.uistate.ContentState
import io.github.solcott.uistate.circuit.produceRetainedContentState

@Inject
class RecipesProducer(private val recipeRepository: RecipeRepository) {

  @Composable
  fun produceBySearchTerm(searchTerm: String, retryTrigger: Int): ContentState<List<Recipe>> =
    produceRetainedContentState(emptyList(), searchTerm, retryTrigger) {
      recipeRepository.searchRecipes(searchTerm)
    }

  @Composable
  fun produceByCategory(category: String, retryTrigger: Int): ContentState<List<Recipe>> =
    produceRetainedContentState(emptyList(), category, retryTrigger) {
      recipeRepository.recipesByCategory(category)
    }

  // A Set, not vararg: the parameter is a produceRetainedContentState key, and an Array compares by
  // identity, so a vararg call site would allocate a fresh key on every recomposition and restart
  // the collection each pass.
  @Composable
  fun produceByIngredients(
    ingredients: Set<String>,
    retryTrigger: Int,
  ): ContentState<List<Recipe>> =
    produceRetainedContentState(emptyList(), ingredients, retryTrigger) {
      recipeRepository.recipesByIngredients(ingredients)
    }

  @Composable
  fun produceByArea(area: String, retryTrigger: Int): ContentState<List<Recipe>> =
    produceRetainedContentState(emptyList(), area, retryTrigger) {
      recipeRepository.recipesByArea(area)
    }

  @Composable
  fun produceByFavorites(retryTrigger: Int): ContentState<List<Recipe>> =
    produceRetainedContentState(emptyList(), retryTrigger) { recipeRepository.getFavoritesAsFlow() }
}
