package com.boardgamegeek.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R
import com.boardgamegeek.extensions.formatTimestamp
import com.boardgamegeek.extensions.toFormattedString
import com.boardgamegeek.model.GeekList
import com.boardgamegeek.ui.theme.BggAppTheme
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
fun GeekListHeader(geekList: GeekList, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = modifier) {
            ListItemSecondaryText(
                geekList.username,
                icon = painterResource(R.drawable.account_circle_24px),
                contentDescription = stringResource(R.string.author),
                textStyle = MaterialTheme.typography.bodyLarge,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                ListItemSecondaryText(
                    geekList.numberOfItems.toFormattedString(),
                    icon = painterResource(R.drawable.geeklist_24px),
                    contentDescription = stringResource(R.string.number_of_items),
                )
                ListItemVerticalDivider()
                ListItemSecondaryText(
                    geekList.numberOfThumbs.toFormattedString(),
                    icon = painterResource(R.drawable.thumb_up_24px),
                    contentDescription = stringResource(R.string.number_of_thumbs),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                var relativePostTimestamp by remember { mutableStateOf("") }
                var relativeEditTimestamp by remember { mutableStateOf("") }
                LaunchedEffect(Unit) {
                    while (true) {
                        relativePostTimestamp = geekList.postTicks.formatTimestamp(context, includeTime = false, isForumTimestamp = true).toString()
                        relativeEditTimestamp = geekList.editTicks.formatTimestamp(context, includeTime = false, isForumTimestamp = true).toString()
                        delay(30.seconds)
                    }
                }
                ListItemSecondaryText(
                    relativePostTimestamp,
                    icon = painterResource(R.drawable.time_24px),
                    contentDescription = stringResource(R.string.posted),
                )
                if (geekList.postTicks != geekList.editTicks) {
                    ListItemVerticalDivider()
                    ListItemSecondaryText(
                        relativeEditTimestamp,
                        icon = painterResource(R.drawable.time_edit_24px),
                        contentDescription = stringResource(R.string.edited),
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
                postTicks = System.currentTimeMillis() - 100_000_000L,
                editTicks = System.currentTimeMillis() - 10_000_000L,
                items = emptyList(),
                comments = emptyList(),
            ),
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
