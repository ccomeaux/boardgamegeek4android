package com.boardgamegeek.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.boardgamegeek.ui.theme.BggAppTheme

@Composable
fun BggLoadingIndicatorBox(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        BggLoadingIndicator(
            Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun BggLoadingIndicator(modifier: Modifier = Modifier) {
    CircularProgressIndicator(
        modifier = modifier.size(64.dp),
        strokeWidth = 8.dp,
        strokeCap = StrokeCap.Round,
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
    )
}

@PreviewLightDark
@Composable
private fun LoadingIndicatorPreview() {
    BggAppTheme {
        BggLoadingIndicator()
    }
}

@PreviewLightDark
@Composable
private fun LoadingIndicatorBoxPreview() {
    BggAppTheme {
        BggLoadingIndicatorBox(
            Modifier
                .fillMaxSize()
                .padding(PaddingValues(16.dp))
        )
    }
}
