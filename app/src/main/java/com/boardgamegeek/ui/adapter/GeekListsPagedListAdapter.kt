package com.boardgamegeek.ui.adapter

import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.databinding.RowGeeklistBinding
import com.boardgamegeek.model.GeekList
import com.boardgamegeek.extensions.inflate
import com.boardgamegeek.ui.GeekListActivity

class GeekListsPagedListAdapter : PagingDataAdapter<GeekList, GeekListsPagedListAdapter.GeekListsViewHolder>(diffCallback) {
    companion object {
        val diffCallback = object : DiffUtil.ItemCallback<GeekList>() {
            override fun areItemsTheSame(oldItem: GeekList, newItem: GeekList): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: GeekList, newItem: GeekList): Boolean = oldItem == newItem
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

        fun bind(geekList: GeekList?) {
            if (geekList == null) return
            binding.titleView.text = geekList.title
            binding.creatorView.text = geekList.username
            binding.numberOfItemsView.text = geekList.numberOfItems.toString()
            binding.numberOfThumbsView.text = geekList.numberOfThumbs.toString()
            itemView.setOnClickListener { v -> GeekListActivity.start(v.context, geekList.id, geekList.title) }
        }
    }
}
