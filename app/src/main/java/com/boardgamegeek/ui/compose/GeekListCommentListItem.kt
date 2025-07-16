package com.boardgamegeek.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
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
import kotlinx.coroutines.delay
import timber.log.Timber
import java.text.NumberFormat
import java.util.Locale
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Composable
fun GeekListCommentListItem(comment: GeekListComment, markupConverter: XmlApiMarkupConverter, modifier: Modifier = Modifier) {
    val openAlertDialog = remember { mutableStateOf(false) }
    val numberFormat = NumberFormat.getInstance(Locale.getDefault())
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ListItemDefaults.threeLineHeight)
            .background(MaterialTheme.colorScheme.surface)
            .clickable { openAlertDialog.value = true }
            .padding(ListItemDefaults.tallPaddingValues)
    ) {
        val context = LocalContext.current
        Row(verticalAlignment = Alignment.CenterVertically) {
            ListItemSecondaryText(
                text = comment.username,
                icon = Icons.Outlined.AccountCircle,
                contentDescription = stringResource(R.string.author),
            )
            ListItemVerticalDivider()
            ListItemSecondaryText(
                text = numberFormat.format(comment.numberOfThumbs),
                icon = Icons.Outlined.ThumbUp,
                contentDescription = stringResource(R.string.number_of_thumbs),
            )
            ListItemVerticalDivider()
            var relativePostTimestamp by remember { mutableStateOf("") }
            var relativeEditTimestamp by remember { mutableStateOf("") }
            ListItemSecondaryText(
                text = relativePostTimestamp,
                icon = Icons.Outlined.Schedule,
                contentDescription = stringResource(R.string.posted),
            )
            if (comment.editDate != comment.postDate) {
                ListItemVerticalDivider()
                ListItemSecondaryText(
                    text = relativeEditTimestamp,
                    icon = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.edited),
                )
            }
            LaunchedEffect(Unit) {
                while (true) {
                    relativePostTimestamp = comment.postDate.formatTimestamp(context, includeTime = false, isForumTimestamp = false).toString()
                    relativeEditTimestamp = comment.editDate.formatTimestamp(context, includeTime = false, isForumTimestamp = false).toString()
                    delay(30.seconds)
                }
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
        GeekListCommentListItem(
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
private fun CommentDialog(comment: GeekListComment, markupConverter: XmlApiMarkupConverter, onDismissRequest: () -> Unit) {
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
