package com.boardgamegeek.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.boardgamegeek.extensions.formatTimestamp
import com.boardgamegeek.model.Forum
import com.boardgamegeek.ui.theme.BggAppTheme
import kotlinx.coroutines.delay
import java.text.NumberFormat
import kotlin.time.Duration.Companion.seconds

@Composable
fun ForumListItem(forum: Forum, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    if (forum.isHeader) {
        ListHeader(forum.title, modifier)
    } else {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center,
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = ListItemDefaults.twoLineHeight)
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onClick)
                .padding(ListItemDefaults.paddingValues)
        ) {
            ListItemPrimaryText(forum.title, modifier = modifier.padding(bottom = 4.dp))
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
                ListItemSecondaryText(
                    numberFormat.format(forum.numberOfThreads),
                    icon = Icons.Outlined.Forum,
                )
                ListItemVerticalDivider()
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
                ListItemSecondaryText(
                    relativeTimestamp,
                    icon = Icons.Outlined.AccessTime,
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
        ForumListItem(forum)
    }
}

private class ForumPreviewParameterProvider : PreviewParameterProvider<Forum> {
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