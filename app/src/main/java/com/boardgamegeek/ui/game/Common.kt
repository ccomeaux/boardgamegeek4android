package com.boardgamegeek.ui.game

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString

@Composable
fun PrimaryRowText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        maxLines = 1,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier,
    )
}

@Composable
fun SecondaryRowText(annotatedString: AnnotatedString, modifier: Modifier = Modifier) {
    Text(
        text = annotatedString,
        maxLines = 2,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
fun SecondaryRowText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        maxLines = 2,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}