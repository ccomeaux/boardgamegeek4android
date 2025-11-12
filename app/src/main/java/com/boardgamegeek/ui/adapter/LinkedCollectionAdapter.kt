package com.boardgamegeek.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.databinding.RowCollectionBinding
import com.boardgamegeek.entities.BriefGameEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.GameActivity
import kotlin.properties.Delegates

class LinkedCollectionAdapter : RecyclerView.Adapter<LinkedCollectionAdapter.DetailViewHolder>(), AutoUpdatableAdapter {
    init {
        setHasStableIds(true)
    }

    var items: List<BriefGameEntity> by Delegates.observable(emptyList()) { _, old, new ->
        autoNotify(old, new) { o, n ->
            o.gameId == n.gameId
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailViewHolder {
        val binding = RowCollectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DetailViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DetailViewHolder, position: Int) {
        holder.bind(items.getOrNull(position))
    }

    override fun getItemCount(): Int = items.size

    override fun getItemId(position: Int): Long = items.getOrNull(position)?.internalId ?: RecyclerView.NO_ID

    inner class DetailViewHolder(private val binding: RowCollectionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(gameDetail: BriefGameEntity?) {
            gameDetail?.let { entity ->
                binding.name.text = entity.name
                binding.year.text = entity.year.asYear(binding.root.context)
                binding.thumbnail.loadThumbnailInList(entity.thumbnailUrl)
                binding.favorite.isVisible = entity.isFavorite
                val personalRating = entity.personalRating.asPersonalRating(binding.root.context, 0)
                if (personalRating.isNotBlank()) {
                    binding.rating.text = personalRating
                    binding.rating.setTextViewBackground(entity.personalRating.toColor(ratingColors))
                    binding.rating.isVisible = true
                } else {
                    binding.rating.isVisible = false
                }
                binding.root.setOnClickListener { GameActivity.start(binding.root.context, entity.gameId, entity.gameName, entity.gameThumbnailUrl, entity.gameHeroImageUrl) }
            }
        }
    }
}
