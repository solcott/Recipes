package com.scottolcott.recipe.model

import kotlin.time.Instant
import kotlinx.serialization.Serializable

/**
 * A cuisine as TheMealDB lists it: the demonym meals are actually tagged with ([area], `Italian`)
 * and the country it names ([country], `Italy`).
 *
 * [country] is nullable because `list.php?a=list` declares `strCountry` nullable and does not
 * return it for every row.
 */
@Serializable data class Area(val area: String, val country: String?, val lastFetched: Instant)
