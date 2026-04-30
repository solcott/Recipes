package com.scottolcott.recipe.storage

import androidx.room3.TypeConverter
import kotlin.time.Instant

class RoomTypeConverters {

  @TypeConverter
  fun instantToString(instant: Instant): String {
    return instant.toString()
  }

  @TypeConverter
  fun stringToInstant(value: String): Instant {
    return Instant.parse(value)
  }
}
