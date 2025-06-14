package com.boardgamegeek.ui.compose

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.boardgamegeek.ui.theme.BggAppTheme

@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
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
        LoadingIndicator()
    }
}
