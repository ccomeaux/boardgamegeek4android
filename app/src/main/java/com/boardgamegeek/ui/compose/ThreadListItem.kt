package com.boardgamegeek.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R
import com.boardgamegeek.extensions.formatTimestamp
import com.boardgamegeek.model.Thread
import com.boardgamegeek.ui.theme.BggAppTheme
import java.text.NumberFormat

@Composable
fun ThreadListItem(thread: Thread, onClick: () -> Unit, modifier: Modifier = Modifier) {
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
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(
                horizontal = dimensionResource(R.dimen.material_margin_horizontal),
                vertical = dimensionResource(R.dimen.material_margin_vertical),
            )
            .then(modifier)
    ) {
        Text(
            thread.subject,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 4.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.AccountCircle,
                contentDescription = stringResource(R.string.author),
                modifier = iconModifier,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = thread.author,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            VerticalDivider(dividerModifier)
            Icon(
                Icons.Outlined.Forum,
                contentDescription = stringResource(R.string.replies),
                modifier = iconModifier,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = numberFormat.format(thread.numberOfArticles - 1),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            VerticalDivider(dividerModifier)
            Icon(
                Icons.Outlined.AccessTime,
                contentDescription = stringResource(R.string.posted),
                modifier = iconModifier,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = thread.lastPostDate.formatTimestamp(LocalContext.current, includeTime = false, isForumTimestamp = true).toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
            subject = "This is a fairly long thread subject, but I've seen longer",
            author = "aldie",
            numberOfArticles = 42,
            lastPostDate = System.currentTimeMillis(),
        )
    )
}