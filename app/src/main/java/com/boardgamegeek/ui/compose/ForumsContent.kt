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
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
    nestedScrollConnection: NestedScrollConnection? = null,
    onItemClick: (forum: Forum) -> Unit = {},
) {
    when {
        forums == null -> {
            BggLoadingIndicatorBox(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (nestedScrollConnection == null) Modifier else Modifier.nestedScroll(nestedScrollConnection))
                    .padding(contentPadding)
            )
        }
        forums.isEmpty() -> {
            EmptyContent(
                stringResource(R.string.empty_forums),
                painterResource(R.drawable.forum_24px),
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (nestedScrollConnection == null) Modifier else Modifier.nestedScroll(nestedScrollConnection))
                    .padding(contentPadding),
            )
        }
        else -> {
            LazyColumn(
                // HACK the nestedScrollConnection isn't working, so this height is fixed to prent crashes
                modifier = Modifier
                    .height(600.dp)
                    .then(if (nestedScrollConnection == null) Modifier else Modifier.nestedScroll(nestedScrollConnection))
                    .padding(contentPadding)
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
                            onClick = { onItemClick(forum) },
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
                val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
                ListItemSecondaryText(
                    numberFormat.format(forum.numberOfThreads),
                    icon = painterResource(R.drawable.forum_24px),
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
                    icon = painterResource(R.drawable.time_24px),
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