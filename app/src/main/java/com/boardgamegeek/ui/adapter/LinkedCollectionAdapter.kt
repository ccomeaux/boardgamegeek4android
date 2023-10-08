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

    init {
        setHasStableIds(true)
    }

    var items: List<CollectionItem>
        get() = currentList
        set(value) {
            submitList(value)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailViewHolder {
        return DetailViewHolder(parent.inflate(R.layout.row_collection))
    }

    override fun onBindViewHolder(holder: DetailViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemId(position: Int): Long = getItem(position).internalId

    inner class DetailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val binding = RowCollectionBinding.bind(itemView)

        fun bind(collectionItem: CollectionItem?) {
            collectionItem?.let {
                binding.nameView.text = it.collectionName.ifBlank { it.gameName }
                binding.yearView.text = it.yearPublished.asYear(itemView.context)
                binding.thumbnailView.loadThumbnail(it.thumbnailUrl.ifBlank { it.gameThumbnailUrl })
                binding.favoriteView.isVisible = it.isFavorite
                binding.ratingView.text = it.rating.asPersonalRating(itemView.context)
                binding.ratingView.setTextViewBackground(it.rating.toColor(BggColors.ratingColors))
                itemView.setOnClickListener { _ ->
                    GameActivity.start(
                        itemView.context,
                        it.gameId,
                        it.gameName,
                        it.gameThumbnailUrl,
                        it.gameHeroImageUrl
                    )
                }
            }
        }
    }
}
