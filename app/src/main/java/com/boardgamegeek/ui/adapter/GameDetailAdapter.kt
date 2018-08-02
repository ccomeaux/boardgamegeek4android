package com.boardgamegeek.ui.adapter

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.boardgamegeek.R
import com.boardgamegeek.entities.GameDetailEntity
import com.boardgamegeek.extensions.inflate
import com.boardgamegeek.ui.GameActivity
import com.boardgamegeek.ui.ProducerActivity
import com.boardgamegeek.ui.viewmodel.GameViewModel
import kotlinx.android.synthetic.main.row_text.view.*
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
        return DetailViewHolder(parent.inflate(R.layout.row_text))
    }

    override fun onBindViewHolder(holder: DetailViewHolder, position: Int) {
        holder.bind(items.getOrNull(position))
    }

    override fun getItemCount(): Int = items.size

    override fun getItemId(position: Int): Long = items.getOrNull(position)?.id?.toLong() ?: RecyclerView.NO_ID

    inner class DetailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(gameDetail: GameDetailEntity?) {
            gameDetail?.let { game ->
                itemView.titleView?.text = game.name
                when (type) {
                    GameViewModel.ProducerType.EXPANSIONS,
                    GameViewModel.ProducerType.BASE_GAMES -> itemView.setOnClickListener { GameActivity.start(itemView.context, game.id, game.name) }
                    GameViewModel.ProducerType.DESIGNER,
                    GameViewModel.ProducerType.ARTIST,
                    GameViewModel.ProducerType.PUBLISHER -> itemView.setOnClickListener { ProducerActivity.start(itemView.context, type, game.id, game.name) }
                    else -> {
                        itemView.setOnClickListener { }
                        itemView.isClickable = false
                    }
                }
            }
        }
    }
}
