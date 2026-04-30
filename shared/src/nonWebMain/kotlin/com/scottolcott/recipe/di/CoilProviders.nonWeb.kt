package com.scottolcott.recipe.di

import coil3.disk.DiskCache
import okio.FileSystem

actual fun newDiskCache(): DiskCache? {
  return DiskCache.Builder()
    .directory(FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "image_cache")
    .maxSizeBytes(MAX_CACHE_SIZE)
    .build()
}
