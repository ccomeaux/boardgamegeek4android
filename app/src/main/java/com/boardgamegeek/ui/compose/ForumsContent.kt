package com.boardgamegeek.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
fun ForumsContent(
    forums: Map<String, List<Forum>>?,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onItemClick: (forum: Forum, header: String) -> Unit = { _, _ -> },
) {
    when {
        forums == null -> {
            BggLoadingIndicatorBox(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            )
        }
        forums.isEmpty() -> {
            EmptyContent(
                stringResource(R.string.empty_forums),
                painterResource(R.drawable.forum_24px),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            )
        }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
            ) {
                forums.forEach { (headerText, forums) ->
                    if (headerText.isNotEmpty()) {
                        stickyHeader {
                            ListHeader(headerText)
                        }
                    }
                    itemsIndexed(
                        items = forums,
                        key = { _, forum -> forum.id }
                    ) { index, forum ->
                        ForumListItem(
                            forum = forum,
                            modifier = Modifier,
                            onClick = { onItemClick(forum, headerText) },
                        )
                        if (index < forums.lastIndex)
                            HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun ForumListItem(forum: Forum, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
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
                if (forum.numberOfThreads == 0) {
                    ListItemSecondaryText(stringResource(R.string.empty_forum))
                } else {
                    val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
                    ListItemSecondaryText(
                        numberFormat.format(forum.numberOfThreads),
                        icon = painterResource(R.drawable.forum_24px),
                    )
                    ListItemVerticalDivider()
                    val context = LocalContext.current
                    var relativeTimestamp by remember { mutableStateOf("") }
                    LaunchedEffect(forum.lastPostDateTime) {
                        while (true) {
                            relativeTimestamp = forum.lastPostDateTime.formatTimestamp(context, includeTime = false, isForumTimestamp = true).toString()
                            delay(60.seconds)
                        }
                    }
                    ListItemSecondaryText(
                        relativeTimestamp,
                        icon = painterResource(R.drawable.time_24px),
                    )
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
        ),
        Forum(
            id = 1,
            title = "Ghost Forum",
            numberOfThreads = 0,
            lastPostDateTime = 0L,
            isHeader = false,
        )
    )
}

@PreviewLightDark
@Composable
private fun ForumsContentPreview() {
    BggAppTheme {
        ForumsContent(
            forums = mapOf(
                "Main Category" to listOf(
                    Forum(
                        id = 1,
                        title = "General",
                        numberOfThreads = 17,
                        lastPostDateTime = System.currentTimeMillis() - 1_000_000L,
                        isHeader = false,
                    ),
                    Forum(
                        id = 2,
                        title = "Rules",
                        numberOfThreads = 3,
                        lastPostDateTime = System.currentTimeMillis() - 100_000_000L,
                        isHeader = false,
                    )
                ),
                "Secondary Category" to listOf(
                    Forum(
                        id = 3,
                        title = "Strategy",
                        numberOfThreads = 120,
                        lastPostDateTime = System.currentTimeMillis() - 10_000_000L,
                        isHeader = false,
                    ),
                    Forum(
                        id = 4,
                        title = "Variants",
                        numberOfThreads = 42,
                        lastPostDateTime = System.currentTimeMillis() - 2_000_000L,
                        isHeader = false,
                    )
                )
            )
        )
    }
}
