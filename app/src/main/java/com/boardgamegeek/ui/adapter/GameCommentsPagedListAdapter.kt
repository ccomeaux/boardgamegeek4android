package com.boardgamegeek.ui.adapter

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.databinding.RowCommentBinding
import com.boardgamegeek.entities.GameCommentEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.util.XmlApiMarkupConverter

class GameCommentsPagedListAdapter : PagingDataAdapter<GameCommentEntity, GameCommentsPagedListAdapter.CommentViewHolder>(diffCallback) {
    companion object {
        val diffCallback = object : DiffUtil.ItemCallback<GameCommentEntity>() {
            override fun areItemsTheSame(oldItem: GameCommentEntity, newItem: GameCommentEntity) = oldItem.username == newItem.username

            override fun areContentsTheSame(oldItem: GameCommentEntity, newItem: GameCommentEntity) = oldItem == newItem
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

        fun bind(entity: GameCommentEntity?) {
            if (entity != null) {
                binding.usernameView.text = entity.username
                binding.ratingView.text = entity.rating.asPersonalRating(itemView.context)
                binding.ratingView.setTextViewBackground(entity.rating.toColor(BggColors.ratingColors))
                binding.commentView.setTextMaybeHtml(markupConverter.toHtml(entity.comment))
                binding.commentView.isVisible = binding.commentView.text.isNotBlank()
                binding.root.isVisible = true
            } else
                binding.root.isVisible = false
        }
    }
}
