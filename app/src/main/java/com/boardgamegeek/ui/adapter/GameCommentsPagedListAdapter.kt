package com.boardgamegeek.ui.adapter

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.databinding.RowCommentBinding
import com.boardgamegeek.entities.GameComment
import com.boardgamegeek.extensions.*
import com.boardgamegeek.util.XmlApiMarkupConverter

class GameCommentsPagedListAdapter : PagingDataAdapter<GameComment, GameCommentsPagedListAdapter.CommentViewHolder>(diffCallback) {
    companion object {
        val diffCallback = object : DiffUtil.ItemCallback<GameComment>() {
            override fun areItemsTheSame(oldItem: GameComment, newItem: GameComment) = oldItem.username == newItem.username

            override fun areContentsTheSame(oldItem: GameComment, newItem: GameComment) = oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        return CommentViewHolder(parent.inflate(R.layout.row_comment))
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val binding = RowCommentBinding.bind(itemView)
        private val markupConverter = XmlApiMarkupConverter(itemView.context)

        fun bind(gameComment: GameComment?) {
            if (gameComment != null) {
                binding.usernameView.text = gameComment.username
                binding.ratingView.text = gameComment.rating.asPersonalRating(itemView.context)
                binding.ratingView.setTextViewBackground(gameComment.rating.toColor(BggColors.ratingColors))
                binding.commentView.setTextMaybeHtml(markupConverter.toHtml(gameComment.comment))
                binding.commentView.isVisible = binding.commentView.text.isNotBlank()
                binding.root.isVisible = true
            } else
                binding.root.isVisible = false
        }
    }
}
