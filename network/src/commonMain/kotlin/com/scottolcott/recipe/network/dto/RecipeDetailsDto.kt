package com.scottolcott.recipe.network.dto

import com.scottolcott.recipe.model.RecipeId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response wrapper for filter.php (returns basic meal info only). */
@Serializable data class RecipeBasicResponseDto(val meals: List<RecipeBasicDto>?)

/** Response wrapper for search.php, lookup.php, random.php (returns full meal details). */
@Serializable data class RecipeFullResponseDto(val meals: List<RecipeFullDto>?)

/** Common identity fields shared by both API response shapes. */
sealed interface RecipeDto {
  val id: RecipeId
  val name: String
  val thumbnail: String
}

@Serializable
data class RecipeBasicDto(
  @SerialName("idMeal") override val id: RecipeId,
  @SerialName("strMeal") override val name: String,
  @SerialName("strMealThumb") override val thumbnail: String,
) : RecipeDto

@Serializable
data class RecipeFullDto(
  @SerialName("idMeal") override val id: RecipeId,
  @SerialName("strMeal") override val name: String,
  @SerialName("strMealAlternate") val alternateName: String? = null,
  @SerialName("strCategory") val category: String,
  @SerialName("strArea") val area: String,
  @SerialName("strInstructions") val instructions: String,
  @SerialName("strMealThumb") override val thumbnail: String,
  @SerialName("strTags") val tags: String? = null,
  @SerialName("strYoutube") val youtube: String? = null,
  @SerialName("strSource") val source: String? = null,
  @SerialName("strImageSource") val imageSource: String? = null,
  @SerialName("strCreativeCommonsConfirmed") val creativeCommonsConfirmed: String? = null,
  @SerialName("dateModified") val dateModified: String? = null,

  // Ingredients and Measures
  // These are nullable as the API may return empty strings or nulls for unused slots
  @SerialName("strIngredient1") val ingredient1: String? = null,
  @SerialName("strMeasure1") val measure1: String? = null,
  @SerialName("strIngredient2") val ingredient2: String? = null,
  @SerialName("strMeasure2") val measure2: String? = null,
  @SerialName("strIngredient3") val ingredient3: String? = null,
  @SerialName("strMeasure3") val measure3: String? = null,
  @SerialName("strIngredient4") val ingredient4: String? = null,
  @SerialName("strMeasure4") val measure4: String? = null,
  @SerialName("strIngredient5") val ingredient5: String? = null,
  @SerialName("strMeasure5") val measure5: String? = null,
  @SerialName("strIngredient6") val ingredient6: String? = null,
  @SerialName("strMeasure6") val measure6: String? = null,
  @SerialName("strIngredient7") val ingredient7: String? = null,
  @SerialName("strMeasure7") val measure7: String? = null,
  @SerialName("strIngredient8") val ingredient8: String? = null,
  @SerialName("strMeasure8") val measure8: String? = null,
  @SerialName("strIngredient9") val ingredient9: String? = null,
  @SerialName("strMeasure9") val measure9: String? = null,
  @SerialName("strIngredient10") val ingredient10: String? = null,
  @SerialName("strMeasure10") val measure10: String? = null,
  @SerialName("strIngredient11") val ingredient11: String? = null,
  @SerialName("strMeasure11") val measure11: String? = null,
  @SerialName("strIngredient12") val ingredient12: String? = null,
  @SerialName("strMeasure12") val measure12: String? = null,
  @SerialName("strIngredient13") val ingredient13: String? = null,
  @SerialName("strMeasure13") val measure13: String? = null,
  @SerialName("strIngredient14") val ingredient14: String? = null,
  @SerialName("strMeasure14") val measure14: String? = null,
  @SerialName("strIngredient15") val ingredient15: String? = null,
  @SerialName("strMeasure15") val measure15: String? = null,
  @SerialName("strIngredient16") val ingredient16: String? = null,
  @SerialName("strMeasure16") val measure16: String? = null,
  @SerialName("strIngredient17") val ingredient17: String? = null,
  @SerialName("strMeasure17") val measure17: String? = null,
  @SerialName("strIngredient18") val ingredient18: String? = null,
  @SerialName("strMeasure18") val measure18: String? = null,
  @SerialName("strIngredient19") val ingredient19: String? = null,
  @SerialName("strMeasure19") val measure19: String? = null,
  @SerialName("strIngredient20") val ingredient20: String? = null,
  @SerialName("strMeasure20") val measure20: String? = null,
) : RecipeDto {
  /** Convert to the basic DTO (useful when you only need summary info). */
  fun toBasicDto() = RecipeBasicDto(id = id, name = name, thumbnail = thumbnail)
}
