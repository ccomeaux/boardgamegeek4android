package com.boardgamegeek.ui.adapter

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.databinding.RowCollectionBinding
import com.boardgamegeek.entities.BriefGameEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.GameActivity

class LinkedCollectionAdapter :
    ListAdapter<BriefGameEntity, LinkedCollectionAdapter.DetailViewHolder>(
        object : DiffUtil.ItemCallback<BriefGameEntity>() {
            override fun areItemsTheSame(oldItem: BriefGameEntity, newItem: BriefGameEntity) = oldItem.gameId == newItem.gameId
            override fun areContentsTheSame(oldItem: BriefGameEntity, newItem: BriefGameEntity) = oldItem == newItem
        }
    ) {

    init {
        setHasStableIds(true)
    }

    var items: List<BriefGameEntity>
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

        fun bind(gameDetail: BriefGameEntity?) {
            gameDetail?.let { entity ->
                binding.nameView.text = entity.name
                binding.yearView.text = entity.year.asYear(itemView.context)
                binding.thumbnailView.loadThumbnailInList(entity.thumbnailUrl)
                binding.favoriteView.isVisible = entity.isFavorite
                val personalRating = entity.personalRating.asPersonalRating(itemView.context, 0)
                if (personalRating.isNotBlank()) {
                    binding.ratingView.text = personalRating
                    binding.ratingView.setTextViewBackground(entity.personalRating.toColor(BggColors.ratingColors))
                    binding.ratingView.isVisible = true
                } else {
                    binding.ratingView.isVisible = false
                }
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
