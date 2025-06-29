package com.boardgamegeek.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R
import com.boardgamegeek.extensions.formatTimestamp
import com.boardgamegeek.model.GeekList
import com.boardgamegeek.ui.theme.BggAppTheme
import java.text.NumberFormat
import java.util.Locale

@Composable
fun GeekListHeader(geekList: GeekList, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val numberFormat = NumberFormat.getInstance(Locale.getDefault())
    val dividerModifier = Modifier
        .size(18.dp)
        .padding(horizontal = 8.dp)
    val iconModifier = Modifier
        .size(18.dp)
        .padding(end = 8.dp)
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = modifier) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.AccountCircle,
                    contentDescription = stringResource(R.string.author),
                    modifier = iconModifier
                )
                Text(
                    text = geekList.username,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Outlined.List,
                    contentDescription = stringResource(R.string.number_of_items),
                    modifier = iconModifier
                )
                Text(
                    text = geekList.numberOfItems.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                )
                VerticalDivider(dividerModifier)
                Icon(
                    Icons.Outlined.ThumbUp,
                    contentDescription = stringResource(R.string.number_of_thumbs),
                    modifier = iconModifier,
                )
                Text(
                    text = numberFormat.format(geekList.numberOfThumbs),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Schedule,
                    contentDescription = stringResource(R.string.posted),
                    modifier = iconModifier,
                )
                Text(
                    text = geekList.postTicks.formatTimestamp(context).toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                )
                if (geekList.postTicks != geekList.editTicks) {
                    VerticalDivider(dividerModifier)
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.edited),
                        modifier = iconModifier,
                    )
                    Text(
                        text = geekList.editTicks.formatTimestamp(context).toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun GeekListHeaderPreview() {
    BggAppTheme {
        GeekListHeader(
            GeekList(
                id = 123,
                title = "My GeekList",
                username = "ccomeaux",
                description = "This is a description",
                numberOfItems = 42,
                numberOfThumbs = 11,
                postTicks = 1234567890L,
                editTicks = 12345678901L,
                items = emptyList(),
                comments = emptyList(),
            ),
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
