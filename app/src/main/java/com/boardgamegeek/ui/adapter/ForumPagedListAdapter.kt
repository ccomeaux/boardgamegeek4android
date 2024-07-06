package com.boardgamegeek.ui.adapter

import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.databinding.RowForumThreadBinding
import com.boardgamegeek.model.Forum
import com.boardgamegeek.model.Thread
import com.boardgamegeek.extensions.inflate
import com.boardgamegeek.extensions.toFormattedString
import com.boardgamegeek.ui.ThreadActivity

class ForumPagedListAdapter(
    private val forumId: Int,
    private val forumTitle: String,
    private val objectId: Int,
    private val objectName: String,
    private val objectType: Forum.Type,
) : PagingDataAdapter<Thread, ForumPagedListAdapter.ForumViewHolder>(diffCallback) {

    companion object {
        val diffCallback = object : DiffUtil.ItemCallback<Thread>() {
            override fun areItemsTheSame(oldItem: Thread, newItem: Thread) = oldItem.threadId == newItem.threadId

            override fun areContentsTheSame(oldItem: Thread, newItem: Thread) = oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForumViewHolder {
        return ForumViewHolder(parent.inflate(R.layout.row_forum_thread))
    }

    override fun onBindViewHolder(holder: ForumViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ForumViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val binding = RowForumThreadBinding.bind(itemView)

        fun bind(thread: Thread?) {
            binding.subjectView.text = thread?.subject.orEmpty()
            binding.authorView.text = thread?.author.orEmpty()
            binding.numberOfArticlesView.text = ((thread?.numberOfArticles ?: 1) - 1).toFormattedString()
            binding.lastPostDateView.timestamp = thread?.lastPostDate ?: 0L
            itemView.setOnClickListener {
                thread?.let { thread ->
                    ThreadActivity.start(it.context, thread.threadId, thread.subject, forumId, forumTitle, objectId, objectName, objectType)
                }
            }
        }
    }
}
