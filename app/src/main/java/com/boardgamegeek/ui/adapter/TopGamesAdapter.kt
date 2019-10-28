package com.boardgamegeek.ui.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.entities.TopGameEntity
import com.boardgamegeek.extensions.asYear
import com.boardgamegeek.extensions.inflate
import com.boardgamegeek.extensions.loadThumbnailInList
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.GameActivity
import kotlinx.android.synthetic.main.row_top_game.view.*
import kotlin.properties.Delegates

class TopGamesAdapter : RecyclerView.Adapter<TopGamesAdapter.ViewHolder>(), AutoUpdatableAdapter {
    init {
        setHasStableIds(true)
    }

    var results: List<TopGameEntity> by Delegates.observable(emptyList()) { _, old, new ->
        autoNotify(old, new) { o, n ->
            o.id == n.id
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(parent.inflate(R.layout.row_top_game))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(results.getOrNull(position))
    }

    override fun getItemCount() = results.size

    override fun getItemId(position: Int) = (results.getOrNull(position)?.id ?: BggContract.INVALID_ID).toLong()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(game: TopGameEntity?) {
            if (game == null) return
            itemView.nameView.text = game.name
            itemView.yearView.text = game.yearPublished.asYear(itemView.context)
            itemView.rankView.text = game.rank.toString()
            itemView.thumbnailView.loadThumbnailInList(game.thumbnailUrl)

            itemView.setOnClickListener {
                GameActivity.start(itemView.context,
                        game.id,
                        game.name,
                        game.thumbnailUrl)
            }
        }
    }
}
