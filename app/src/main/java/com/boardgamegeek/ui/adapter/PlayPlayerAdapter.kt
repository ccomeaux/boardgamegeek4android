package com.boardgamegeek.ui.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView.LayoutParams
import android.widget.BaseAdapter
import com.boardgamegeek.model.Play
import com.boardgamegeek.model.Player
import com.boardgamegeek.ui.BuddyActivity
import com.boardgamegeek.ui.widget.PlayerRow

class PlayPlayerAdapter(private val context: Context, private var play: Play) : BaseAdapter() {
    fun replace(play: Play) {
        this.play = play
        notifyDataSetChanged()
    }

    override fun isEnabled(position: Int) = false

    override fun getCount() = play.getPlayerCount()

    override fun getItem(position: Int): Any? = play.players.getOrNull(position)

    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val row = convertView as PlayerRow? ?: PlayerRow(context)
        row.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        val player = getItem(position) as Player?
        row.setPlayer(player)
        if (player != null) {
            row.setOnClickListener { BuddyActivity.start(context, player.username, player.name) }
        }
        return row
    }
}
