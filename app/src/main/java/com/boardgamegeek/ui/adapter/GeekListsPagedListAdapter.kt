package com.boardgamegeek.ui.adapter

import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.databinding.RowGeeklistBinding
import com.boardgamegeek.entities.GeekListEntity
import com.boardgamegeek.extensions.inflate
import com.boardgamegeek.ui.GeekListActivity

class GeekListsPagedListAdapter : PagingDataAdapter<GeekListEntity, GeekListsPagedListAdapter.GeekListsViewHolder>(diffCallback) {
    companion object {
        val diffCallback = object : DiffUtil.ItemCallback<GeekListEntity>() {
            override fun areItemsTheSame(oldItem: GeekListEntity, newItem: GeekListEntity): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: GeekListEntity, newItem: GeekListEntity): Boolean = oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GeekListsViewHolder {
        return GeekListsViewHolder(parent.inflate(R.layout.row_geeklist))
    }

    override fun onBindViewHolder(holder: GeekListsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class GeekListsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val binding = RowGeeklistBinding.bind(itemView)

        fun bind(entity: GeekListEntity?) {
            if (entity == null) return
            binding.titleView.text = entity.title
            binding.creatorView.text = entity.username
            binding.numberOfItemsView.text = entity.numberOfItems.toString()
            binding.numberOfThumbsView.text = entity.numberOfThumbs.toString()
            itemView.setOnClickListener { v -> GeekListActivity.start(v.context, entity.id, entity.title) }
        }
    }
}
