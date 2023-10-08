package com.boardgamegeek.ui.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.PorterDuff
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.databinding.RowPlayPlayerBinding
import com.boardgamegeek.model.PlayPlayer
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.BuddyActivity

class PlayPlayerAdapter : RecyclerView.Adapter<PlayPlayerAdapter.PlayerViewHolder>() {
    init {
        setHasStableIds(false)
    }

    var players: List<PlayPlayer> = emptyList()
        @SuppressLint("NotifyDataSetChanged")
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
        private val binding = RowPlayPlayerBinding.bind(itemView)

        fun bind(player: PlayPlayer?) {
            val nameColor = binding.nameView.textColors.defaultColor

            if (player == null) {
                binding.nameView.setTextOrHide(itemView.context.getString(R.string.title_player))
                binding.usernameView.setTextOrHide("")
                binding.scoreView.setTextOrHide("")
                binding.scoreButton.isVisible = false
                binding.ratingView.setTextOrHide("")
                binding.ratingButton.isVisible = false
                binding.colorView.isVisible = false
                binding.teamColorView.setTextOrHide("")
                binding.seatView.setTextOrHide("")
                binding.startingPositionView.setTextOrHide("")
                itemView.setOnClickListener { }
            } else {
                // name & username
                if (player.name.isBlank() && player.username.isBlank()) {
                    val name = if (player.seat == PlayPlayer.SEAT_UNKNOWN)
                        itemView.context.resources.getString(R.string.title_player)
                    else
                        itemView.context.resources.getString(R.string.generic_player, player.seat)
                    binding.nameView.setTextWithStyle(
                        name,
                        player.isNew,
                        player.isWin,
                        ContextCompat.getColor(itemView.context, R.color.secondary_text)
                    )
                    binding.usernameView.isVisible = false
                } else if (player.name.isBlank()) {
                    binding.nameView.setTextWithStyle(player.username, player.isNew, player.isWin, nameColor)
                    binding.usernameView.isVisible = false
                } else {
                    binding.nameView.setTextWithStyle(player.name, player.isNew, player.isWin, nameColor)
                    binding.usernameView.setTextWithStyle(player.username, player.isNew, player.isWin, nameColor)
                }

                // score
                val scoreDescription = if (player.numericScore == null) {
                    player.numericScore.asScore(itemView.context)
                } else {
                    player.score
                }
                binding.scoreView.setTextWithStyle(scoreDescription, false, player.isWin, nameColor)
                binding.scoreButton.setColorFilter(ContextCompat.getColor(itemView.context, R.color.button_under_text), PorterDuff.Mode.SRC_IN)
                binding.scoreButton.isVisible = player.score.isNotEmpty()

                // rating
                binding.ratingButton.setColorFilter(ContextCompat.getColor(itemView.context, R.color.button_under_text), PorterDuff.Mode.SRC_IN)
                if (player.rating == 0.0) {
                    binding.ratingView.isVisible = false
                    binding.ratingButton.isVisible = false
                } else {
                    binding.ratingView.setTextOrHide(player.rating.asPersonalRating(itemView.context))
                    binding.ratingButton.isVisible = true
                }

                // team/color
                val color = player.color.asColorRgb()
                binding.colorView.setColorViewValue(color)
                binding.teamColorView.setTextOrHide(player.color)
                binding.teamColorView.isVisible = color == Color.TRANSPARENT && player.color.isNotBlank()

                // starting position, team/color
                if (player.seat == PlayPlayer.SEAT_UNKNOWN) {
                    binding.seatView.isVisible = false
                    binding.startingPositionView.setTextOrHide(player.startingPosition)
                } else {
                    binding.seatView.setTextColor(color.getTextColor())
                    binding.seatView.setTextOrHide(player.startingPosition)
                    binding.startingPositionView.isVisible = false
                }

                itemView.setOnClickListener { BuddyActivity.start(itemView.context, player.username, player.name) }
            }
        }
    }
}
