package com.boardgamegeek.ui.adapter

import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.ui.GameActivity
import com.boardgamegeek.ui.compose.CollectionItemListItem

class LinkedCollectionAdapter :
    ListAdapter<CollectionItem, LinkedCollectionAdapter.DetailViewHolder>(
        object : DiffUtil.ItemCallback<CollectionItem>() {
            override fun areItemsTheSame(oldItem: CollectionItem, newItem: CollectionItem) = oldItem.gameId == newItem.gameId
            override fun areContentsTheSame(oldItem: CollectionItem, newItem: CollectionItem) = oldItem == newItem
        }
    ) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailViewHolder {
        return DetailViewHolder(ComposeView(parent.context))
    }

    override fun onBindViewHolder(holder: DetailViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it) }
    }

    inner class DetailViewHolder(private val composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {
        fun bind(item: CollectionItem) {
            composeView.setContent {
                CollectionItemListItem(
                    thumbnailUrl = item.robustThumbnailUrl,
                    name = item.robustName,
                    yearPublished = item.yearPublished,
                    isFavorite = item.isFavorite,
                    infoText = null,
                    rating = item.rating,
                    timestamp = null,
                ) {
                    GameActivity.start(
                        itemView.context,
                        item.gameId,
                        item.gameName,
                        item.gameThumbnailUrl,
                        item.gameHeroImageUrl
                    )
                }
            }
        }
    }
}
