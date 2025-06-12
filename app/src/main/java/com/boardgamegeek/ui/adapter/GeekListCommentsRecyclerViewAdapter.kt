package com.boardgamegeek.ui.adapter

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.extensions.formatTimestamp
import com.boardgamegeek.model.GeekListComment
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.util.XmlApiMarkupConverter
import timber.log.Timber
import kotlin.properties.Delegates
import kotlin.time.Duration.Companion.hours
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class GeekListCommentsRecyclerViewAdapter
    : RecyclerView.Adapter<GeekListCommentsRecyclerViewAdapter.CommentViewHolder>(), AutoUpdatableAdapter {
    var comments: List<GeekListComment> by Delegates.observable(emptyList()) { _, old, new ->
        autoNotify(old, new) { o, n ->
            o == n
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        return CommentViewHolder(ComposeView(parent.context))
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        comments.getOrNull(position)?.let { holder.bind(it) }
    }

    override fun getItemCount() = comments.size

    inner class CommentViewHolder(private val composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {
        private val markupConverter = XmlApiMarkupConverter(itemView.context)

        fun bind(comment: GeekListComment) {
            composeView.setContent {
                BggAppTheme {
                    GeekListCommentRow(
                        comment,
                        markupConverter,
                        modifier = Modifier.padding(
                            horizontal = dimensionResource(com.boardgamegeek.R.dimen.material_margin_horizontal),
                            vertical = dimensionResource(com.boardgamegeek.R.dimen.material_margin_vertical),
                        ),
                    )
                }
            }
        }
    }
}

@Composable
fun GeekListCommentRow(comment: GeekListComment, markupConverter: XmlApiMarkupConverter, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val iconModifier = Modifier
        .size(18.dp)
        .padding(end = 8.dp)
    val dividerModifier = Modifier
        .size(18.dp)
        .padding(horizontal = 8.dp)
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = modifier.padding(bottom = 0.dp),
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
}

@Preview(backgroundColor = 0xFF00FF00, showBackground = true)
@Composable
private fun GeekListCommentRowPreview() {
    BggAppTheme {
        val postDate = (System.currentTimeMillis().toDuration(DurationUnit.MILLISECONDS) - 2.hours).inWholeMilliseconds
        val markupConverter = XmlApiMarkupConverter(LocalContext.current)
        GeekListCommentRow(
            GeekListComment(
                postDate = postDate,
                editDate = postDate,
                numberOfThumbs = 11,
                username = "ccomeaux",
                content = "I like this game. It is fun. Boy howdy. I sure do like to play it!"
            ),
            markupConverter,
            Modifier.padding(
                horizontal = dimensionResource(com.boardgamegeek.R.dimen.material_margin_horizontal),
                vertical = dimensionResource(com.boardgamegeek.R.dimen.material_margin_vertical),
            ),
        )
    }
}

@Preview(backgroundColor = 0xFF00FF00, showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun GeekListCommentRowPreviewEditedDark() {
    BggAppTheme {
        val postDate = (System.currentTimeMillis().toDuration(DurationUnit.MILLISECONDS) - 2.hours).inWholeMilliseconds
        val markupConverter = XmlApiMarkupConverter(LocalContext.current)
        GeekListCommentRow(
            GeekListComment(
                postDate = postDate,
                editDate = (postDate.toDuration(DurationUnit.MILLISECONDS) + 1.hours).inWholeMilliseconds,
                numberOfThumbs = 12,
                username = "ccomeaux",
                content = "I like this game. It is fun. Boy howdy. I sure do like to play it!".repeat(10)
            ),
            markupConverter,
            Modifier.padding(
                horizontal = dimensionResource(com.boardgamegeek.R.dimen.material_margin_horizontal),
                vertical = dimensionResource(com.boardgamegeek.R.dimen.material_margin_vertical),
            ),
        )
    }
}