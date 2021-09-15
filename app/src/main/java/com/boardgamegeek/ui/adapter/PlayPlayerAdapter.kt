package com.boardgamegeek.ui.adapter

import android.graphics.Color
import android.graphics.PorterDuff
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.entities.PlayPlayerEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.BuddyActivity
import kotlinx.android.synthetic.main.row_play_player.view.*
import java.text.DecimalFormat

class PlayPlayerAdapter : RecyclerView.Adapter<PlayPlayerAdapter.PlayerViewHolder>() {
    init {
        setHasStableIds(false)
    }

    var players: List<PlayPlayerEntity> = emptyList()
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

    override fun getItemCount() = players.size

    inner class PlayerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ratingFormat = DecimalFormat("0.0######")

        fun bind(player: PlayPlayerEntity?) {
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
                if (player.name.isBlank() && player.username.isBlank()) {
                    val name = if (player.seat == PlayPlayerEntity.SEAT_UNKNOWN)
                        itemView.context.resources.getString(R.string.title_player)
                    else
                        itemView.context.resources.getString(R.string.generic_player, player.seat)
                    itemView.nameView.setText(name, nameTypeface, player.isNew, player.isWin, ContextCompat.getColor(itemView.context, R.color.secondary_text))
                    itemView.usernameView.isVisible = false
                } else if (player.name.isBlank()) {
                    itemView.nameView.setText(player.username, nameTypeface, player.isNew, player.isWin, nameColor)
                    itemView.usernameView.isVisible = false
                } else {
                    itemView.nameView.setText(player.name, nameTypeface, player.isNew, player.isWin, nameColor)
                    itemView.usernameView.setText(player.username, usernameTypeface, player.isNew, player.isWin, nameColor)
                }

                // score
                val scoreDescription = if (player.numericScore == null) {
                    player.numericScore.asScore(itemView.context)
                } else {
                    player.score.orEmpty()
                }
                itemView.scoreView.setText(scoreDescription, scoreTypeface, false, player.isWin, nameColor)
                itemView.scoreButton.setColorFilter(ContextCompat.getColor(itemView.context, R.color.button_under_text), PorterDuff.Mode.SRC_IN)
                itemView.scoreButton.isVisible = !player.score.isNullOrEmpty()

                // rating
                itemView.ratingButton.setColorFilter(ContextCompat.getColor(itemView.context, R.color.button_under_text), PorterDuff.Mode.SRC_IN)
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
                if (player.seat == PlayPlayerEntity.SEAT_UNKNOWN) {
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
