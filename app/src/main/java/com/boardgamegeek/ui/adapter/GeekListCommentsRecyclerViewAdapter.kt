package com.boardgamegeek.ui.adapter

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.databinding.RowGeeklistCommentBinding
import com.boardgamegeek.entities.GeekListCommentEntity
import com.boardgamegeek.extensions.inflate
import kotlin.properties.Delegates

class GeekListCommentsRecyclerViewAdapter
    : RecyclerView.Adapter<GeekListCommentsRecyclerViewAdapter.CommentViewHolder>(), AutoUpdatableAdapter {
    var comments: List<GeekListCommentEntity> by Delegates.observable(emptyList()) { _, old, new ->
        autoNotify(old, new) { o, n ->
            o == n
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        return CommentViewHolder(parent.inflate(R.layout.row_geeklist_comment))
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(comments.getOrNull(position))
    }

    override fun getItemCount() = comments.size

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = RowGeeklistCommentBinding.bind(itemView)

        fun bind(comment: GeekListCommentEntity?) {
            comment?.let {
                binding.usernameView.text = it.username
                binding.numberOfThumbsView.text = it.numberOfThumbs.toString()
                binding.postedDateView.timestamp = it.postDate
                binding.editedDateView.timestamp = it.editDate
                binding.editedDateView.isVisible = it.editDate != it.postDate
                binding.datetimeDividerView.isVisible = it.editDate != it.postDate
                binding.commentView.text = it.content
            }
        }
    }
}
