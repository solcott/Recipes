package com.scottolcott.recipe

enum class AppPlatform {
  Android,
  Ios,
  Desktop,
  Web,
}

expect val currentPlatform: AppPlatform

fun isIos() = currentPlatform == AppPlatform.Ios

fun isDesktop() = currentPlatform == AppPlatform.Desktop

fun isWeb() = currentPlatform == AppPlatform.Web

fun isAndroid() = currentPlatform == AppPlatform.Android
