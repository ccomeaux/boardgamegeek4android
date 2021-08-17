package com.boardgamegeek.ui.adapter

import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.entities.GameCommentEntity
import com.boardgamegeek.extensions.*
import kotlinx.android.synthetic.main.row_comment.view.*

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
        fun bind(comment: GameCommentEntity?) {
            itemView.usernameView.text = comment?.username.orEmpty()
            itemView.ratingView.text = comment?.rating?.asRating(itemView.context).orEmpty()
            itemView.ratingView.setTextViewBackground(
                (comment?.rating ?: 0.0).toColor(ratingColors)
            )
            itemView.commentView.setTextOrHide(comment?.comment)
        }
    }
}
