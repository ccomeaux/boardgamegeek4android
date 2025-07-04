package com.boardgamegeek.ui.adapter

import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.model.Forum
import com.boardgamegeek.model.Thread
import com.boardgamegeek.ui.ThreadActivity
import com.boardgamegeek.ui.compose.ThreadListItem

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
        return ForumViewHolder(ComposeView(parent.context))
    }

    override fun onBindViewHolder(holder: ForumViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it) }
    }

    inner class ForumViewHolder(val view: ComposeView) : RecyclerView.ViewHolder(view) {
        fun bind(thread: Thread) {
            view.setContent {
                ThreadListItem(
                    thread = thread,
                    onClick = {
                        ThreadActivity.start(itemView.context, thread.threadId, thread.subject, forumId, forumTitle, objectId, objectName, objectType)
                    }
                )
            }
        }
    }
}
