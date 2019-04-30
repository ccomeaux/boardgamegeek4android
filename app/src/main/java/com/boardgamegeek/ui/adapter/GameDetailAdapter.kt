package com.boardgamegeek.ui.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.entities.GameDetailEntity
import com.boardgamegeek.extensions.inflate
import com.boardgamegeek.extensions.setTextOrHide
import com.boardgamegeek.ui.GameActivity
import com.boardgamegeek.ui.PersonActivity
import com.boardgamegeek.ui.viewmodel.GameViewModel
import kotlinx.android.synthetic.main.row_game_detail.view.*
import kotlin.properties.Delegates

class GameDetailAdapter : RecyclerView.Adapter<GameDetailAdapter.DetailViewHolder>(), AutoUpdatableAdapter {
    init {
        setHasStableIds(true)
    }

    var type: GameViewModel.ProducerType by Delegates.observable(GameViewModel.ProducerType.UNKNOWN) { _, old, new ->
        if (old != new) notifyDataSetChanged()
    }

    var items: List<GameDetailEntity> by Delegates.observable(emptyList()) { _, old, new ->
        autoNotify(old, new) { o, n ->
            o.id == n.id
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailViewHolder {
        return DetailViewHolder(parent.inflate(R.layout.row_game_detail))
    }

    override fun onBindViewHolder(holder: DetailViewHolder, position: Int) {
        holder.bind(items.getOrNull(position))
    }

    override fun getItemCount(): Int = items.size

    override fun getItemId(position: Int): Long = items.getOrNull(position)?.id?.toLong() ?: RecyclerView.NO_ID

    inner class DetailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(gameDetail: GameDetailEntity?) {
            gameDetail?.let { entity ->
                itemView.nameView?.text = entity.name
                itemView.descriptionView.setTextOrHide(entity.description)
                when (type) {
                    GameViewModel.ProducerType.EXPANSIONS,
                    GameViewModel.ProducerType.BASE_GAMES -> itemView.setOnClickListener { GameActivity.start(itemView.context, entity.id, entity.name) }
                    GameViewModel.ProducerType.PUBLISHER -> itemView.setOnClickListener { PersonActivity.startForPublisher(itemView.context, entity.id, entity.name) }
                    GameViewModel.ProducerType.ARTIST -> itemView.setOnClickListener { PersonActivity.startForArtist(itemView.context, entity.id, entity.name) }
                    GameViewModel.ProducerType.DESIGNER -> itemView.setOnClickListener { PersonActivity.startForDesigner(itemView.context, entity.id, entity.name) }
                    else -> {
                        itemView.setOnClickListener { }
                        itemView.isClickable = false
                    }
                }
            }
        }
    }
}
