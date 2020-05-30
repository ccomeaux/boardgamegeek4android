package com.boardgamegeek.ui.adapter

import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.entities.ForumEntity
import com.boardgamegeek.entities.ThreadEntity
import com.boardgamegeek.extensions.inflate
import com.boardgamegeek.ui.ThreadActivity
import kotlinx.android.synthetic.main.row_forum_thread.view.*
import java.text.NumberFormat

class ForumPagedListAdapter(private val forumId: Int, private val forumTitle: String, private val objectId: Int, private val objectName: String, private val objectType: ForumEntity.ForumType)
    : PagedListAdapter<ThreadEntity, ForumPagedListAdapter.ForumViewHolder>(AsyncDifferConfig.Builder(diffCallback).build()) {

    companion object {
        val diffCallback = object : DiffUtil.ItemCallback<ThreadEntity>() {
            override fun areItemsTheSame(oldItem: ThreadEntity, newItem: ThreadEntity): Boolean =
                    oldItem.threadId == newItem.threadId

            override fun areContentsTheSame(oldItem: ThreadEntity, newItem: ThreadEntity): Boolean =
                    oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForumViewHolder {
        return ForumViewHolder(parent.inflate(R.layout.row_forum_thread))
    }

    override fun onBindViewHolder(holder: ForumViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ForumViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(thread: ThreadEntity?) {
            itemView.subject.text = thread?.subject ?: ""
            itemView.author.text = thread?.author ?: ""
            val replies = (thread?.numberOfArticles ?: 1) - 1
            itemView.number_of_articles.text = NumberFormat.getInstance().format(replies.toLong())
            itemView.last_post_date.timestamp = thread?.lastPostDate ?: 0L
            if (thread == null)
                itemView.setOnClickListener {}
            else
                itemView.setOnClickListener {
                    ThreadActivity.start(it.context, thread.threadId, thread.subject, forumId, forumTitle, objectId, objectName, objectType)
                }
        }
    }
}
