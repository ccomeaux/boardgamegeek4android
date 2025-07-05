package com.boardgamegeek.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R
import com.boardgamegeek.extensions.formatTimestamp
import com.boardgamegeek.model.Forum
import com.boardgamegeek.ui.theme.BggAppTheme
import kotlinx.coroutines.delay
import java.text.NumberFormat
import kotlin.time.Duration.Companion.seconds

@Composable
fun ForumListItem(forum: Forum, onClick: () -> Unit, modifier: Modifier = Modifier) {
    if (forum.isHeader) {
        ListHeader(forum.title, modifier)
    } else {
        val iconModifier = Modifier
            .size(18.dp)
            .padding(end = 8.dp)
        val dividerModifier = Modifier
            .size(18.dp)
            .padding(horizontal = 8.dp)
        val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .clickable(onClick = onClick)
                .padding(
                    horizontal = dimensionResource(R.dimen.material_margin_horizontal),
                    vertical = dimensionResource(R.dimen.material_margin_vertical),
                )
                .then(modifier)
        ) {
            Text(
                text = forum.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.Forum,
                    contentDescription = null,
                    modifier = iconModifier,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = numberFormat.format(forum.numberOfThreads),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                VerticalDivider(dividerModifier)
                Icon(
                    Icons.Outlined.AccessTime,
                    contentDescription = null,
                    modifier = iconModifier,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val context = LocalContext.current
                var relativeTimestamp by remember {
                    mutableStateOf(
                        forum.lastPostDateTime.formatTimestamp(
                            context,
                            includeTime = false,
                            isForumTimestamp = true
                        ).toString()
                    )
                }
                Text(
                    text = relativeTimestamp,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LaunchedEffect(Unit) {
                    while (true) {
                        delay(30.seconds)
                        relativeTimestamp = forum.lastPostDateTime.formatTimestamp(context, includeTime = false, isForumTimestamp = true).toString()
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ForumListItemPreview(
    @PreviewParameter(ForumPreviewParameterProvider::class) forum: Forum
) {
    BggAppTheme {
        ForumListItem(
            forum,
            {},
        )
    }
}

class ForumPreviewParameterProvider : PreviewParameterProvider<Forum> {
    override val values = sequenceOf(
        Forum(
            id = 1,
            title = "General",
            numberOfThreads = 17,
            lastPostDateTime = System.currentTimeMillis(),
            isHeader = true,
        ),
        Forum(
            id = 1,
            title = "Test Forum",
            numberOfThreads = 17,
            lastPostDateTime = System.currentTimeMillis(),
            isHeader = false,
        )
    )
}