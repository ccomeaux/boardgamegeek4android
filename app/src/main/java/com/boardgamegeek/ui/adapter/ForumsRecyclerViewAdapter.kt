package com.boardgamegeek.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.databinding.RowForumBinding
import com.boardgamegeek.databinding.RowForumHeaderBinding
import com.boardgamegeek.entities.ForumEntity
import com.boardgamegeek.ui.ForumActivity
import java.text.NumberFormat

class ForumsRecyclerViewAdapter(
    private val objectId: Int,
    private val objectName: String,
    private val objectType: ForumEntity.ForumType
) : RecyclerView.Adapter<ForumsRecyclerViewAdapter.ForumViewHolder>() {
    init {
        setHasStableIds(true)
    }

    var forums: List<ForumEntity> = emptyList()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForumViewHolder {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            ITEM_VIEW_TYPE_FORUM -> ForumViewHolder.ForumItemViewHolder(inflater.inflate(R.layout.row_forum, parent, false))
            ITEM_VIEW_TYPE_HEADER -> ForumViewHolder.HeaderViewHolder(inflater.inflate(R.layout.row_forum_header, parent, false))
            else -> ForumViewHolder.HeaderViewHolder(inflater.inflate(R.layout.row_header, parent, false))
        }
    }

    override fun onBindViewHolder(holder: ForumViewHolder, position: Int) {
        return when (holder) {
            is ForumViewHolder.HeaderViewHolder -> holder.bind(forums.getOrNull(position))
            is ForumViewHolder.ForumItemViewHolder -> holder.bind(forums.getOrNull(position), objectId, objectName, objectType)
        }
    }

    override fun getItemCount() = forums.size

    override fun getItemViewType(position: Int): Int {
        return if (forums.getOrNull(position)?.isHeader != false) ITEM_VIEW_TYPE_HEADER else ITEM_VIEW_TYPE_FORUM
    }

    override fun getItemId(position: Int) = position.toLong()

    companion object {
        private const val ITEM_VIEW_TYPE_FORUM = 0
        private const val ITEM_VIEW_TYPE_HEADER = 1
        private val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
    }

    sealed class ForumViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        class ForumItemViewHolder(itemView: View) : ForumViewHolder(itemView) {
            private val binding = RowForumBinding.bind(itemView)

            fun bind(forum: ForumEntity?, objectId: Int, objectName: String, objectType: ForumEntity.ForumType) {
                if (forum == null) return
                binding.titleView.text = forum.title
                binding.numberOfThreadsView.text = numberFormat.format(forum.numberOfThreads.toLong())
                binding.lastPostDateView.timestamp = forum.lastPostDateTime
                itemView.setOnClickListener { ForumActivity.start(itemView.context, forum.id, forum.title, objectId, objectName, objectType) }
            }
        }

        class HeaderViewHolder(itemView: View) : ForumViewHolder(itemView) {
            private val binding = RowForumHeaderBinding.bind(itemView)

            fun bind(forum: ForumEntity?) {
                binding.headerView.text = forum?.title
            }
        }
    }
}
