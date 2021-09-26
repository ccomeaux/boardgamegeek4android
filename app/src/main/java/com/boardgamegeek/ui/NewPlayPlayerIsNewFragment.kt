package com.boardgamegeek.ui

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.entities.NewPlayPlayerEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.viewmodel.NewPlayViewModel
import kotlinx.android.synthetic.main.fragment_new_play_player_is_new.*
import kotlinx.android.synthetic.main.row_new_play_player_is_new.view.*
import kotlin.properties.Delegates

class NewPlayPlayerIsNewFragment : Fragment(R.layout.fragment_new_play_player_is_new) {
    private val viewModel by activityViewModels<NewPlayViewModel>()
    private val adapter: PlayersAdapter by lazy { PlayersAdapter(viewModel) }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.setSubtitle(R.string.title_new_players)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.adapter = adapter

        viewModel.mightBeNewPlayers.observe(viewLifecycleOwner, { entity ->
            adapter.players = entity.sortedBy { it.seat }
        })

        nextButton.setOnClickListener {
            viewModel.finishPlayerIsNew()
        }
    }

    private class Diff(private val oldList: List<NewPlayPlayerEntity>, private val newList: List<NewPlayPlayerEntity>) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].isNew == newList[newItemPosition].isNew
        }
    }

    private class PlayersAdapter(private val viewModel: NewPlayViewModel)
        : RecyclerView.Adapter<PlayersAdapter.PlayersViewHolder>() {

        var players: List<NewPlayPlayerEntity> by Delegates.observable(emptyList()) { _, oldValue, newValue ->
            val diffCallback = Diff(oldValue, newValue)
            val diffResult = DiffUtil.calculateDiff(diffCallback)
            diffResult.dispatchUpdatesTo(this)
        }

        init {
            setHasStableIds(true)
        }

        override fun getItemCount() = players.size

        override fun getItemId(position: Int): Long {
            return players.getOrNull(position)?.id?.hashCode()?.toLong() ?: RecyclerView.NO_ID
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayersViewHolder {
            return PlayersViewHolder(parent.inflate(R.layout.row_new_play_player_is_new))
        }

        override fun onBindViewHolder(holder: PlayersViewHolder, position: Int) {
            holder.bind(position)
        }

        inner class PlayersViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            fun bind(position: Int) {
                val entity = players.getOrNull(position)
                entity?.let { player ->
                    itemView.nameView.text = player.name
                    itemView.usernameView.setTextOrHide(player.username)

                    if (player.color.isBlank()) {
                        itemView.colorView.isInvisible = true
                        itemView.teamView.isVisible = false
                        itemView.seatView.setTextColor(Color.TRANSPARENT.getTextColor())
                    } else {
                        val color = player.color.asColorRgb()
                        if (color == Color.TRANSPARENT) {
                            itemView.colorView.isInvisible = true
                            itemView.teamView.setTextOrHide(player.color)
                        } else {
                            itemView.colorView.setColorViewValue(color)
                            itemView.colorView.isVisible = true
                            itemView.teamView.isVisible = false
                        }
                        itemView.seatView.setTextColor(color.getTextColor())
                    }

                    if (player.seat == null) {
                        itemView.sortView.setTextOrHide(player.sortOrder)
                        itemView.seatView.isInvisible = true
                    } else {
                        itemView.sortView.isVisible = false
                        itemView.seatView.text = player.seat.toString()
                        itemView.seatView.isVisible = true
                    }

                    itemView.isNewCheckBox.isChecked = player.isNew

                    itemView.isNewCheckBox.setOnCheckedChangeListener { _, isChecked ->
                        viewModel.addIsNewToPlayer(player.id, isChecked)
                        itemView.isNewCheckBox.setOnCheckedChangeListener { _, _ -> }
                    }
                }
            }
        }
    }
}