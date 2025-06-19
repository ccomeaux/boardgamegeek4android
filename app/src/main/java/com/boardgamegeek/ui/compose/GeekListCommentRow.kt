package com.boardgamegeek.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.boardgamegeek.extensions.formatTimestamp
import com.boardgamegeek.model.GeekListComment
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.util.XmlApiMarkupConverter
import timber.log.Timber
import kotlin.time.Duration.Companion.hours
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Composable
fun GeekListCommentRow(comment: GeekListComment, markupConverter: XmlApiMarkupConverter, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val iconModifier = Modifier
        .size(18.dp)
        .padding(end = 8.dp)
    val dividerModifier = Modifier
        .size(18.dp)
        .padding(horizontal = 8.dp)
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.AccountCircle,
                contentDescription = null,
                modifier = iconModifier
            )
            Text(
                text = comment.username,
                style = MaterialTheme.typography.bodyMedium,
            )
            VerticalDivider(dividerModifier)
            Icon(
                Icons.Outlined.ThumbUp,
                contentDescription = null,
                modifier = iconModifier
            )
            Text(
                text = comment.numberOfThumbs.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            VerticalDivider(dividerModifier)
            Icon(
                Icons.Outlined.Schedule,
                contentDescription = null,
                modifier = iconModifier
            )
            Text(
                // TODO figure out how to force a recompose each minute for relative time to update correctly
                text = comment.postDate.formatTimestamp(context, includeTime = false, isForumTimestamp = false).toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            if (comment.editDate != comment.postDate) {
                VerticalDivider(dividerModifier)
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = null,
                    modifier = iconModifier
                )
                Text(
                    text = comment.editDate.formatTimestamp(context, includeTime = false, isForumTimestamp = false).toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
        Text(
            text = AnnotatedString.fromHtml(markupConverter.toHtml(comment.content)).also { Timber.i("CPC $it") },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp, bottom = 0.dp),
            maxLines = 5,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@PreviewLightDark
@Composable
private fun GeekListCommentRowPreviewEditedDark(
    @PreviewParameter(GeekListCommentPreviewParameterProvider::class) geekListComment: GeekListComment,
) {
    BggAppTheme {
        val markupConverter = XmlApiMarkupConverter(LocalContext.current)
        GeekListCommentRow(
            geekListComment,
            markupConverter,
            Modifier.padding(
                horizontal = dimensionResource(com.boardgamegeek.R.dimen.material_margin_horizontal),
                vertical = dimensionResource(com.boardgamegeek.R.dimen.material_margin_vertical),
            ),
        )
    }
}

class GeekListCommentPreviewParameterProvider : PreviewParameterProvider<GeekListComment> {
    private val postDate = (System.currentTimeMillis().toDuration(DurationUnit.MILLISECONDS) - 2.hours).inWholeMilliseconds
    override val values = sequenceOf(
        GeekListComment(
            postDate = postDate,
            editDate = postDate,
            numberOfThumbs = 11,
            username = "ccomeaux",
            content = "I like this game. It is fun. Boy howdy. I sure do like to play it!"
        ),
        GeekListComment(
            postDate = postDate,
            editDate = (postDate.toDuration(DurationUnit.MILLISECONDS) + 1.hours).inWholeMilliseconds,
            numberOfThumbs = 12,
            username = "ccomeaux",
            content = "I like this game. It is fun. Boy howdy. I sure do like to play it!".repeat(10)
        ),
    )
}