package com.scottolcott.recipe.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource

@Composable
fun ErrorDisplay(onRetryClick: () -> Unit, modifier: Modifier = Modifier) {
  Column(modifier = modifier) {
    Text(stringResource(Res.string.an_error_occurred))
    Button(onRetryClick) { Text(stringResource(Res.string.retry)) }
  }
}
