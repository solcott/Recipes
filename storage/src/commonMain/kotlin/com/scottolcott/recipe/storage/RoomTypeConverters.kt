package com.scottolcott.recipe.storage

import androidx.room3.ColumnTypeConverter
import kotlin.time.Instant

class RoomTypeConverters {

  @ColumnTypeConverter
  fun instantToString(instant: Instant): String {
    return instant.toString()
  }

  @ColumnTypeConverter
  fun stringToInstant(value: String): Instant {
    return Instant.parse(value)
  }
}
