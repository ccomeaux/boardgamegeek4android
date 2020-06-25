package com.boardgamegeek.ui.adapter

import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.entities.GeekListEntity
import com.boardgamegeek.extensions.inflate
import com.boardgamegeek.ui.GeekListActivity.Companion.start
import kotlinx.android.synthetic.main.row_geeklist.view.*

class GeekListsPagedListAdapter : PagedListAdapter<GeekListEntity, GeekListsPagedListAdapter.GeekListsViewHolder>(AsyncDifferConfig.Builder(diffCallback).build()) {

    companion object {
        val diffCallback = object : DiffUtil.ItemCallback<GeekListEntity>() {
            override fun areItemsTheSame(oldItem: GeekListEntity, newItem: GeekListEntity): Boolean =
                    oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: GeekListEntity, newItem: GeekListEntity): Boolean =
                    oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GeekListsViewHolder {
        return GeekListsViewHolder(parent.inflate(R.layout.row_geeklist))
    }

    override fun onBindViewHolder(holder: GeekListsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class GeekListsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(entity: GeekListEntity?) {
            if (entity == null) return
            itemView.titleView.text = entity.title
            itemView.creatorView.text = entity.username
            itemView.numberOfItemsView.text = entity.numberOfItems.toString()
            itemView.numberOfThumbsView.text = entity.numberOfThumbs.toString()
            itemView.setOnClickListener { v -> start(v.context, entity.id, entity.title) }
        }
    }
}
