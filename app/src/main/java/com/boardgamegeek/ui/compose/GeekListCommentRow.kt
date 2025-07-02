package com.boardgamegeek.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.window.Dialog
import com.boardgamegeek.R
import com.boardgamegeek.extensions.formatTimestamp
import com.boardgamegeek.model.GeekListComment
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.util.XmlApiMarkupConverter
import timber.log.Timber
import java.text.NumberFormat
import java.util.Locale
import kotlin.time.Duration.Companion.hours
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Composable
fun GeekListCommentRow(comment: GeekListComment, markupConverter: XmlApiMarkupConverter, modifier: Modifier = Modifier) {
    val openAlertDialog = remember { mutableStateOf(false) }
    val numberFormat = NumberFormat.getInstance(Locale.getDefault())
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable {
                openAlertDialog.value = true
            }
            .padding(
                horizontal = dimensionResource(R.dimen.material_margin_horizontal),
                vertical = dimensionResource(R.dimen.material_margin_vertical),
            ),
    ) {
        val context = LocalContext.current
        val iconModifier = Modifier
            .size(18.dp)
            .padding(end = 8.dp)
        val dividerModifier = Modifier
            .size(18.dp)
            .padding(horizontal = 8.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.AccountCircle,
                contentDescription = null,
                modifier = iconModifier,
                tint = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = comment.username,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            VerticalDivider(dividerModifier)
            Icon(
                Icons.Outlined.ThumbUp,
                contentDescription = null,
                modifier = iconModifier,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = numberFormat.format(comment.numberOfThumbs),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            VerticalDivider(dividerModifier)
            Icon(
                Icons.Outlined.Schedule,
                contentDescription = null,
                modifier = iconModifier,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    modifier = iconModifier,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
            text = AnnotatedString.fromHtml(markupConverter.toHtml(comment.content)), // TODO this seems to always have a trailing new line
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 5,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 0.dp)
        )
    }
    if (openAlertDialog.value) {
        CommentDialog(
            comment = comment,
            markupConverter = markupConverter,
            onDismissRequest = { openAlertDialog.value = false }
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
            editDate = postDate,
            numberOfThumbs = 11,
            username = "ccomeaux",
            content = "Brief comment."
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

@Composable
fun CommentDialog(comment: GeekListComment, markupConverter: XmlApiMarkupConverter, onDismissRequest: () -> Unit) {
    val numberFormat = NumberFormat.getInstance(Locale.getDefault())
    Dialog(onDismissRequest = { onDismissRequest() }) {
        Card(modifier = Modifier.padding(vertical = 24.dp)) {
            val iconModifier = Modifier
                .size(18.dp)
                .padding(end = 8.dp)
            Row(
                modifier = Modifier
                    .padding(horizontal = dimensionResource(R.dimen.material_margin_dialog))
                    .padding(top = dimensionResource(R.dimen.material_margin_dialog), bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.AccountCircle,
                    contentDescription = null,
                    modifier = iconModifier,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = comment.username,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    Icons.Outlined.ThumbUp,
                    contentDescription = null,
                    modifier = iconModifier,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = numberFormat.format(comment.numberOfThumbs),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val htmlString = markupConverter.toHtml(comment.content)
            Text(
                text = AnnotatedString.fromHtml(htmlString).also { Timber.i("HTML: $it") },
                modifier = Modifier
                    .padding(bottom = dimensionResource(R.dimen.material_margin_dialog))
                    .padding(horizontal = dimensionResource(R.dimen.material_margin_dialog))
                    .verticalScroll(rememberScrollState())
                    .wrapContentSize(Alignment.Center),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun CommentDialogPreview(
    @PreviewParameter(GeekListCommentPreviewParameterProvider::class) geekListComment: GeekListComment,
) {
    BggAppTheme {
        val markupConverter = XmlApiMarkupConverter(LocalContext.current)
        CommentDialog(geekListComment, markupConverter) {

        }
    }
}
