package com.boardgamegeek.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R
import com.boardgamegeek.extensions.formatTimestamp
import com.boardgamegeek.model.Thread
import com.boardgamegeek.ui.theme.BggAppTheme
import kotlinx.coroutines.delay
import java.text.NumberFormat
import kotlin.time.Duration.Companion.seconds

@Composable
fun ThreadListItem(thread: Thread, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ListItemDefaults.threeLineHeight)
            .background(color = MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(ListItemDefaults.tallPaddingValues)
    ) {
        ListItemPrimaryText(thread.subject, modifier = modifier.padding(bottom = 4.dp))
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ListItemSecondaryText(
                text = thread.author,
                icon = Icons.Outlined.AccountCircle,
                contentDescription = stringResource(R.string.author),
            )
            ListItemVerticalDivider()
            val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
            ListItemSecondaryText(
                text = numberFormat.format(thread.numberOfArticles - 1),
                icon = Icons.Outlined.Forum,
                contentDescription = stringResource(R.string.replies),
            )
            ListItemVerticalDivider()
            val context = LocalContext.current
            var relativeTimestamp by remember {
                mutableStateOf(
                    thread.lastPostDate.formatTimestamp(
                        context,
                        includeTime = false,
                        isForumTimestamp = true
                    ).toString()
                )
            }
            ListItemSecondaryText(
                text = relativeTimestamp,
                icon = Icons.Outlined.AccessTime,
                contentDescription = stringResource(R.string.posted),
            )
            LaunchedEffect(Unit) {
                while (true) {
                    delay(30.seconds)
                    relativeTimestamp = thread.lastPostDate.formatTimestamp(context, includeTime = false, isForumTimestamp = true).toString()
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ThreadListItemPreview(
    @PreviewParameter(ThreadPreviewParameterProvider::class) thread: Thread
) {
    BggAppTheme {
        ThreadListItem(
            thread,
            {},
        )
    }
}

private class ThreadPreviewParameterProvider : PreviewParameterProvider<Thread> {
    override val values = sequenceOf(
        Thread(
            threadId = 1,
            subject = "Good Game",
            author = "ccomeaux",
            numberOfArticles = 17,
            lastPostDate = System.currentTimeMillis(),
        ),
        Thread(
            threadId = 1,
            subject = "This is a fairly long thread subject, but I've seen longer. Not much longer mind you.",
            author = "aldie",
            numberOfArticles = 42,
            lastPostDate = System.currentTimeMillis(),
        )
    )
}