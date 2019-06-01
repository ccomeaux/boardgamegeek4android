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

    override fun getItemId(position: Int): Long = items.getOrNull(position)?.gameId?.toLong() ?: RecyclerView.NO_ID

    inner class DetailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(gameDetail: BriefGameEntity?) {
            gameDetail?.let { entity ->
                itemView.name.text = entity.collectionName
                itemView.year.text = entity.yearPublished.asYear(itemView.context)
                itemView.thumbnail.loadUrl(entity.collectionThumbnailUrl)
                itemView.favorite.isVisible = entity.isFavorite
                val personalRating = entity.personalRating.asPersonalRating(itemView.context, 0)
                if (personalRating.isNotBlank()) {
                    itemView.rating.text = personalRating
                    itemView.rating.setTextViewBackground(entity.personalRating.toColor(ratingColors))
                    itemView.rating.isVisible = true
                } else {
                    itemView.rating.isVisible = false
                }
                itemView.setOnClickListener { GameActivity.start(itemView.context, entity.gameId, entity.gameName, entity.gameThumbnailUrl, entity.gameHeroImageUrl) }
            }
        }
    }
}
