package com.scottolcott.recipe.domain.producer

import androidx.compose.runtime.Composable
import com.scottolcott.recipe.model.Recipe
import com.scottolcott.recipe.repository.RecipeRepository
import dev.zacsweers.metro.Inject
import io.github.solcott.uistate.ContentState
import io.github.solcott.uistate.circuit.produceRetainedContentState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Inject
class RecipesProducer(private val recipeRepository: RecipeRepository) {

  @Composable
  fun produceBySearchTerm(
    searchTerm: String,
    retryTrigger: Int,
  ): ContentState<ImmutableList<Recipe>> =
    produceRetainedContentState(persistentListOf(), searchTerm, retryTrigger) {
      recipeRepository.searchRecipes(searchTerm)
    }

  @Composable
  fun produceByCategory(category: String, retryTrigger: Int): ContentState<ImmutableList<Recipe>> =
    produceRetainedContentState(persistentListOf(), category, retryTrigger) {
      recipeRepository.recipesByCategory(category)
    }

  // A Set, not vararg: the parameter is a produceRetainedContentState key, and an Array compares by
  // identity, so a vararg call site would allocate a fresh key on every recomposition and restart
  // the collection each pass.
  //
  // UnstableCollections wants an ImmutableSet, and for a composable that emits UI it would be
  // right. This one returns a value, so the compiler marks it neither restartable nor skippable --
  // the stability report has it as a bare `fun`, against `restartable skippable` for the :ui
  // composables. The parameter's stability is inert here, while an ImmutableSet would cost either a
  // persistent-set copy per recomposition at the call site or a kotlinx-serialization serializer
  // for RecipesScreen.ByIngredient, which has none for the immutable collections.
  @Suppress("UnstableCollections")
  @Composable
  fun produceByIngredients(
    ingredients: Set<String>,
    retryTrigger: Int,
  ): ContentState<ImmutableList<Recipe>> =
    produceRetainedContentState(persistentListOf(), ingredients, retryTrigger) {
      recipeRepository.recipesByIngredients(ingredients)
    }

  @Composable
  fun produceByArea(area: String, retryTrigger: Int): ContentState<ImmutableList<Recipe>> =
    produceRetainedContentState(persistentListOf(), area, retryTrigger) {
      recipeRepository.recipesByArea(area)
    }

  @Composable
  fun produceByFavorites(retryTrigger: Int): ContentState<ImmutableList<Recipe>> =
    produceRetainedContentState(persistentListOf(), retryTrigger) {
      recipeRepository.getFavoritesAsFlow()
    }
}
