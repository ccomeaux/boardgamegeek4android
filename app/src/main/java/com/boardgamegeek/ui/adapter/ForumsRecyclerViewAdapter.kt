package com.boardgamegeek.ui.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.boardgamegeek.R
import com.boardgamegeek.model.Forum
import com.boardgamegeek.ui.ForumActivity
import com.boardgamegeek.ui.widget.TimestampView
import kotlinx.android.synthetic.main.row_forum.view.*
import kotlinx.android.synthetic.main.row_forum_header.view.*
import java.text.NumberFormat

private const val ITEM_VIEW_TYPE_FORUM = 0
private const val ITEM_VIEW_TYPE_HEADER = 1

class ForumsRecyclerViewAdapter(context: Context, private val forums: List<Forum>, private val gameId: Int, private val gameName: String) : RecyclerView.Adapter<ForumViewHolder>() {
    private val inflater: LayoutInflater = LayoutInflater.from(context)

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForumViewHolder {
        return when (viewType) {
            ITEM_VIEW_TYPE_FORUM -> ForumViewHolder.ForumItemViewHolder(inflater.inflate(R.layout.row_forum, parent, false))
            ITEM_VIEW_TYPE_HEADER -> ForumViewHolder.HeaderViewHolder(inflater.inflate(R.layout.row_forum_header, parent, false))
            else -> ForumViewHolder.HeaderViewHolder(inflater.inflate(R.layout.row_header, parent, false))
        }
    }

    override fun onBindViewHolder(holder: ForumViewHolder, position: Int) {
        return when (holder) {
            is ForumViewHolder.HeaderViewHolder -> holder.bind(forums.getOrNull(position))
            is ForumViewHolder.ForumItemViewHolder -> holder.bind(forums.getOrNull(position), gameId, gameName)
        }
    }

    override fun getItemCount() = forums.size

    override fun getItemViewType(position: Int): Int {
        val forum = forums.getOrNull(position)
        return if (forum?.isHeader != false) ITEM_VIEW_TYPE_HEADER else ITEM_VIEW_TYPE_FORUM
    }

    override fun getItemId(position: Int) = position.toLong()
}

sealed class ForumViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    class ForumItemViewHolder(itemView: View) : ForumViewHolder(itemView) {
        private val numberFormat = NumberFormat.getNumberInstance()

        private val titleView: TextView = itemView.title
        private val numberOfThreadsView: TextView = itemView.numberOfThreads
        private val lastPostDateView: TimestampView = itemView.lastPostDate

        fun bind(forum: Forum?, gameId: Int, gameName: String) {
            if (forum == null) return

            titleView.text = forum.title
            numberOfThreadsView.text = numberFormat.format(forum.numberOfThreads.toLong())
            lastPostDateView.timestamp = forum.lastPostDate()

            itemView.setOnClickListener { ForumActivity.start(it.context, forum.id, forum.title, gameId, gameName) }
        }
    }

    class HeaderViewHolder(itemView: View) : ForumViewHolder(itemView) {
        private val header: TextView = itemView.header

        fun bind(forum: Forum?) {
            header.text = forum?.title
        }
    }
}


