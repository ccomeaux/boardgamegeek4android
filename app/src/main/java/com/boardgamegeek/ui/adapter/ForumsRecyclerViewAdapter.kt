package com.boardgamegeek.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.databinding.RowForumBinding
import com.boardgamegeek.databinding.RowForumHeaderBinding
import com.boardgamegeek.entities.ForumEntity
import com.boardgamegeek.ui.ForumActivity
import java.text.NumberFormat

private const val ITEM_VIEW_TYPE_FORUM = 0
private const val ITEM_VIEW_TYPE_HEADER = 1

class ForumsRecyclerViewAdapter(
        private val objectId: Int,
        private val objectName: String?,
        private val objectType: ForumEntity.ForumType
) : RecyclerView.Adapter<ForumsRecyclerViewAdapter.ForumViewHolder>() {
    init {
        setHasStableIds(true)
    }

    var forums: List<ForumEntity> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForumViewHolder {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            ITEM_VIEW_TYPE_FORUM -> {
                val binding = RowForumBinding.inflate(inflater, parent, false)
                ForumViewHolder.ForumItemViewHolder(binding)
            }
            ITEM_VIEW_TYPE_HEADER -> {
                val binding = RowForumHeaderBinding.inflate(inflater, parent, false)
                ForumViewHolder.HeaderViewHolder(binding)
            }
            else -> {
                val binding = RowForumHeaderBinding.inflate(inflater, parent, false)
                ForumViewHolder.HeaderViewHolder(binding)
            }
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
        val forum = forums.getOrNull(position)
        return if (forum?.isHeader != false) ITEM_VIEW_TYPE_HEADER else ITEM_VIEW_TYPE_FORUM
    }

    override fun getItemId(position: Int) = position.toLong()

    companion object {
        val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
    }

    sealed class ForumViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        class ForumItemViewHolder(private val binding: RowForumBinding) : ForumViewHolder(binding.root) {

            fun bind(forum: ForumEntity?, objectId: Int, objectName: String?, objectType: ForumEntity.ForumType) {
                if (forum == null) return
                binding.title.text = forum.title
                binding.numberOfThreads.text = numberFormat.format(forum.numberOfThreads.toLong())
                binding.lastPostDate.timestamp = forum.lastPostDateTime
                binding.root.setOnClickListener { ForumActivity.start(binding.root.context, forum.id, forum.title, objectId, objectName, objectType) }
            }
        }

        class HeaderViewHolder(private val binding: RowForumHeaderBinding) : ForumViewHolder(binding.root) {
            fun bind(forum: ForumEntity?) {
                binding.header.text = forum?.title
            }
        }
    }
}
