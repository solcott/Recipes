package com.scottolcott.recipe.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewWrapperProvider
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.scottolcott.recipe.LocalWindowSizeClass
import com.scottolcott.recipe.ui.theme.RecipeAppTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

class ThemeWrapper : PreviewWrapperProvider {
  @Composable
  override fun Wrap(content: @Composable (() -> Unit)) {
    val windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    RecipeAppTheme {
      CompositionLocalProvider(LocalWindowSizeClass provides windowSizeClass) {
        Scaffold(
          Modifier.fillMaxSize(),
          topBar = {
            TopAppBar(
              title = { Text(stringResource(Res.string.recipes)) },
              navigationIcon = {
                IconButton(
                  {},
                  enabled = false,
                  colors =
                    IconButtonDefaults.iconButtonColors(
                      disabledContentColor = LocalContentColor.current
                    ),
                ) {
                  Icon(
                    painter = painterResource(Res.drawable.chef_hat_24px),
                    contentDescription = null,
                  )
                }
              },
              actions = {
                IconButton(onClick = {}) {
                  Icon(
                    painter = painterResource(Res.drawable.favorite_24px_filled),
                    contentDescription = null,
                  )
                }
                IconButton(onClick = {}) {
                  Icon(
                    painter = painterResource(Res.drawable.search_24px),
                    contentDescription = null,
                  )
                }
              },
              modifier = Modifier.fillMaxWidth(),
              colors =
                TopAppBarDefaults.topAppBarColors(
                  MaterialTheme.colorScheme.primary,
                  titleContentColor = MaterialTheme.colorScheme.onPrimary,
                  navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                  actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
          },
          contentWindowInsets = WindowInsets(0.dp),
        ) { paddingValues ->
          Box(Modifier.padding(paddingValues)) { content() }
        }
      }
    }
  }
}
