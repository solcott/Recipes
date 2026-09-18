package com.scottolcott.recipe.model

import androidx.compose.runtime.Immutable
import io.github.solcott.kmp.parcelize.Parcelable
import io.github.solcott.kmp.parcelize.Parcelize
import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

@Immutable @JvmInline @Serializable @Parcelize value class RecipeId(val id: String) : Parcelable
