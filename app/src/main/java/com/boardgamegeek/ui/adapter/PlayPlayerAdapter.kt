package com.boardgamegeek.ui.adapter

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.extensions.*
import com.boardgamegeek.model.Play
import com.boardgamegeek.model.Player
import com.boardgamegeek.ui.BuddyActivity
import kotlinx.android.synthetic.main.row_play_player.view.*
import java.text.DecimalFormat

class PlayPlayerAdapter(private var play: Play) : RecyclerView.Adapter<PlayPlayerAdapter.PlayerViewHolder>() {
    var players: List<Player> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayPlayerAdapter.PlayerViewHolder {
        return PlayerViewHolder(parent.inflate(R.layout.row_play_player))
    }

    override fun onBindViewHolder(holder: PlayPlayerAdapter.PlayerViewHolder, position: Int) {
        holder.bind(players.getOrNull(position))
    }

    override fun getItemCount() = play.getPlayerCount()

    override fun getItemId(position: Int) = position.toLong()

    inner class PlayerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ratingFormat = DecimalFormat("0.0######")

        fun bind(player: Player?) {
            val nameTypeface = itemView.nameView.typeface
            val usernameTypeface = itemView.usernameView.typeface
            val scoreTypeface = itemView.scoreView.typeface
            val nameColor = itemView.nameView.textColors.defaultColor

            if (player == null) {
                itemView.nameView.setTextOrHide(itemView.context.getString(R.string.title_player))
                itemView.usernameView.setTextOrHide("")
                itemView.scoreView.setTextOrHide("")
                itemView.scoreButton.isVisible = false
                itemView.ratingView.setTextOrHide("")
                itemView.ratingButton.isVisible = false
                itemView.colorView.isVisible = false
                itemView.teamColorView.setTextOrHide("")
                itemView.seatView.setTextOrHide("")
                itemView.startingPositionView.setTextOrHide("")
                itemView.setOnClickListener { }
            } else {
                // name & username
                if (player.name.isNullOrBlank() && player.username.isNullOrBlank()) {
                    val name = if (player.seat == Player.SEAT_UNKNOWN)
                        itemView.context.resources.getString(R.string.title_player)
                    else
                        itemView.context.resources.getString(R.string.generic_player, player.seat)
                    itemView.nameView.setText(name, nameTypeface, player.isNew, player.isWin, ContextCompat.getColor(itemView.context, R.color.secondary_text))
                    itemView.usernameView.isVisible = false
                } else if (player.name.isNullOrBlank()) {
                    itemView.nameView.setText(player.username, nameTypeface, player.isNew, player.isWin, nameColor)
                    itemView.usernameView.isVisible = false
                } else {
                    itemView.nameView.setText(player.name, nameTypeface, player.isNew, player.isWin, nameColor)
                    itemView.usernameView.setText(player.username, usernameTypeface, player.isNew, player.isWin, nameColor)
                }

                // score
                itemView.scoreView.setText(player.scoreDescription, scoreTypeface, false, player.isWin, nameColor)
                itemView.scoreButton.isVisible = !player.score.isNullOrEmpty()

                // rating
                if (player.rating == 0.0) {
                    itemView.ratingView.isVisible = false
                    itemView.ratingButton.isVisible = false
                } else {
                    itemView.ratingView.setTextOrHide(player.rating.asScore(itemView.context, format = ratingFormat))
                    itemView.ratingButton.isVisible = true
                }


                // team/color
                val color = player.color.asColorRgb()
                itemView.colorView.setColorViewValue(color)
                itemView.teamColorView.setTextOrHide(player.color)
                itemView.teamColorView.isVisible = color == Color.TRANSPARENT

                // starting position, team/color
                if (player.seat == Player.SEAT_UNKNOWN) {
                    itemView.seatView.isVisible = false
                    itemView.startingPositionView.setTextOrHide(player.startingPosition)
                } else {
                    itemView.seatView.setTextColor(color.getTextColor())
                    itemView.seatView.setTextOrHide(player.startingPosition)
                    itemView.startingPositionView.isVisible = false
                }

                itemView.setOnClickListener { BuddyActivity.start(itemView.context, player.username, player.name) }
            }
        }
    }
}
