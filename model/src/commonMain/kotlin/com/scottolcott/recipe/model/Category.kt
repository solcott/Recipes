package com.scottolcott.recipe.model

import io.github.solcott.kmp.parcelize.Parcelable
import io.github.solcott.kmp.parcelize.Parcelize
import kotlin.jvm.JvmInline
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Parcelize @JvmInline @Serializable value class CategoryId(val value: String) : Parcelable

data class Category(
  val id: CategoryId,
  val name: String,
  val thumb: String,
  val description: String,
  val lastFetched: Instant,
)
