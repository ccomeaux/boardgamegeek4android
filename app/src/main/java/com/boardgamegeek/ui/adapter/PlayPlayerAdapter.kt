package com.boardgamegeek.ui.adapter

import android.view.ViewGroup
import android.widget.AbsListView.LayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.model.Play
import com.boardgamegeek.model.Player
import com.boardgamegeek.ui.BuddyActivity
import com.boardgamegeek.ui.widget.PlayerRow

class PlayPlayerAdapter(private var play: Play) : RecyclerView.Adapter<PlayPlayerAdapter.PlayerViewHolder>() {
    fun replace(play: Play) {
        this.play = play
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayPlayerAdapter.PlayerViewHolder {
        return PlayerViewHolder(PlayerRow(parent.context))
    }

    override fun onBindViewHolder(holder: PlayPlayerAdapter.PlayerViewHolder, position: Int) {
        holder.bind(play.players.getOrNull(position))
    }

    override fun getItemCount() = play.getPlayerCount()

    override fun getItemId(position: Int) = position.toLong()

    inner class PlayerViewHolder(itemView: PlayerRow) : RecyclerView.ViewHolder(itemView) {
        fun bind(player: Player?) {
            itemView.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            (itemView as? PlayerRow)?.let { row ->
                row.isEnabled = false
                row.setPlayer(player)
                player?.let { p -> row.setOnClickListener { BuddyActivity.start(itemView.context, p.username, p.name) } }
            }
        }
    }
}
