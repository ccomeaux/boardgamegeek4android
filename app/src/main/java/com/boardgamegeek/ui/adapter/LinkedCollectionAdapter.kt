package com.boardgamegeek.ui.adapter

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.databinding.RowCollectionBinding
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.GameActivity

class LinkedCollectionAdapter :
    ListAdapter<CollectionItem, LinkedCollectionAdapter.DetailViewHolder>(
        object : DiffUtil.ItemCallback<CollectionItem>() {
            override fun areItemsTheSame(oldItem: CollectionItem, newItem: CollectionItem) = oldItem.gameId == newItem.gameId
            override fun areContentsTheSame(oldItem: CollectionItem, newItem: CollectionItem) = oldItem == newItem
        }
    ) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailViewHolder {
        return DetailViewHolder(parent.inflate(R.layout.row_collection))
    }

    override fun onBindViewHolder(holder: DetailViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DetailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val binding = RowCollectionBinding.bind(itemView)

        fun bind(item: CollectionItem) {
            binding.nameView.text = item.robustName
            binding.yearView.text = item.yearPublished.asYear(itemView.context)
            binding.thumbnailView.loadThumbnail(item.robustThumbnailUrl)
            binding.favoriteView.isVisible = item.isFavorite
            binding.ratingView.text = item.rating.asPersonalRating(itemView.context)
            binding.ratingView.setTextViewBackground(item.rating.toColor(BggColors.ratingColors))
            itemView.setOnClickListener { _ ->
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
