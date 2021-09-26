package com.boardgamegeek.ui.adapter

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.entities.BriefGameEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.GameActivity
import kotlinx.android.synthetic.main.row_collection.view.*
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
        return DetailViewHolder(parent.inflate(R.layout.row_collection))
    }

    override fun onBindViewHolder(holder: DetailViewHolder, position: Int) {
        holder.bind(items.getOrNull(position))
    }

    override fun getItemCount(): Int = items.size

    override fun getItemId(position: Int): Long = items.getOrNull(position)?.internalId ?: RecyclerView.NO_ID

    inner class DetailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(gameDetail: BriefGameEntity?) {
            gameDetail?.let { entity ->
                itemView.nameView.text = entity.name
                itemView.yearView.text = entity.year.asYear(itemView.context)
                itemView.thumbnailView.loadThumbnailInList(entity.thumbnailUrl)
                itemView.favoriteView.isVisible = entity.isFavorite
                val personalRating = entity.personalRating.asPersonalRating(itemView.context, 0)
                if (personalRating.isNotBlank()) {
                    itemView.ratingView.text = personalRating
                    itemView.ratingView.setTextViewBackground(entity.personalRating.toColor(ratingColors))
                    itemView.ratingView.isVisible = true
                } else {
                    itemView.ratingView.isVisible = false
                }
                itemView.setOnClickListener { GameActivity.start(itemView.context, entity.gameId, entity.gameName, entity.gameThumbnailUrl, entity.gameHeroImageUrl) }
            }
        }
    }
}
