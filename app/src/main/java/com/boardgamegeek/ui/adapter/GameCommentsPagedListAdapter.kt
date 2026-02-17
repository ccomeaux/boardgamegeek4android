package com.boardgamegeek.ui.adapter

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.model.GameComment
import com.boardgamegeek.ui.compose.ListItemDefaults
import com.boardgamegeek.ui.compose.ListItemPrimaryText
import com.boardgamegeek.ui.compose.ListItemSecondaryText
import com.boardgamegeek.ui.compose.Rating
import com.boardgamegeek.ui.theme.BggAppTheme

class GameCommentsPagedListAdapter : PagingDataAdapter<GameComment, GameCommentsPagedListAdapter.CommentViewHolder>(diffCallback) {
    companion object {
        val diffCallback = object : DiffUtil.ItemCallback<GameComment>() {
            override fun areItemsTheSame(oldItem: GameComment, newItem: GameComment) = oldItem.username == newItem.username

            override fun areContentsTheSame(oldItem: GameComment, newItem: GameComment) = oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        return CommentViewHolder(ComposeView(parent.context))
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it) }
    }

    class CommentViewHolder(private val composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {
        fun bind(gameComment: GameComment) {
            composeView.setContent {
                BggAppTheme {
                    CommentListItem(gameComment)
                }
            }
        }
    }
}

@Composable
private fun CommentListItem(
    gameComment: GameComment,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ListItemDefaults.twoLineHeight)
            .background(MaterialTheme.colorScheme.surface)
            .padding(ListItemDefaults.paddingValues)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            ListItemPrimaryText(gameComment.username)
            Rating(gameComment.rating)
        }
        if (gameComment.comment.isNotBlank()) {
            ListItemSecondaryText(AnnotatedString.fromHtml(gameComment.comment))
        }
    }
}

@PreviewLightDark
@Composable
private fun CommentListItemPreview(
    @PreviewParameter(CommentPreviewParameterProvider::class) gameComment: GameComment
) {
    BggAppTheme {
        CommentListItem(gameComment)
    }
}

private class CommentPreviewParameterProvider : PreviewParameterProvider<GameComment> {
    override val values = sequenceOf(
        GameComment(
            username = "username",
            rating = 8.5,
            comment = "I like it.",
        ),
        GameComment(
            username = "ccomeaux",
            rating = 10.0,
            comment = "I <b>love</b> it.",
        ),
    )
}
