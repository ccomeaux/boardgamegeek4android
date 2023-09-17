package com.boardgamegeek.ui.adapter

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.databinding.RowCollectionBinding
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.GameActivity

class LinkedCollectionAdapter :
    ListAdapter<CollectionItemEntity, LinkedCollectionAdapter.DetailViewHolder>(
        object : DiffUtil.ItemCallback<CollectionItemEntity>() {
            override fun areItemsTheSame(oldItem: CollectionItemEntity, newItem: CollectionItemEntity) = oldItem.gameId == newItem.gameId
            override fun areContentsTheSame(oldItem: CollectionItemEntity, newItem: CollectionItemEntity) = oldItem == newItem
        }
    ) {

    init {
        setHasStableIds(true)
    }

    var items: List<CollectionItemEntity>
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

        fun bind(gameDetail: CollectionItemEntity?) {
            gameDetail?.let { entity ->
                binding.nameView.text = entity.collectionName.ifBlank { entity.gameName }
                binding.yearView.text = entity.yearPublished.asYear(itemView.context)
                binding.thumbnailView.loadThumbnail(entity.thumbnailUrl.ifBlank { entity.gameThumbnailUrl })
                binding.favoriteView.isVisible = entity.isFavorite
                binding.ratingView.text = entity.rating.asPersonalRating(itemView.context)
                binding.ratingView.setTextViewBackground(entity.rating.toColor(BggColors.ratingColors))
                itemView.setOnClickListener {
                    GameActivity.start(
                        itemView.context,
                        entity.gameId,
                        entity.gameName,
                        entity.gameThumbnailUrl,
                        entity.gameHeroImageUrl
                    )
                }
            }
        }
    }
}
