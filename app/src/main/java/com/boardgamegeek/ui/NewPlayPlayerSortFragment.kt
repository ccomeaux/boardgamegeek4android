package com.boardgamegeek.ui

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.entities.NewPlayPlayerEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.viewmodel.NewPlayViewModel
import kotlinx.android.synthetic.main.fragment_new_play_player_sort.*
import kotlinx.android.synthetic.main.row_new_play_player_sort.view.*
import kotlin.properties.Delegates

class NewPlayPlayerSortFragment : Fragment(R.layout.fragment_new_play_player_sort) {
    private val viewModel by activityViewModels<NewPlayViewModel>()
    private val adapter: PlayersAdapter by lazy { PlayersAdapter(viewModel) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.adapter = adapter

        viewModel.addedPlayers.observe(viewLifecycleOwner, Observer { entity ->
            if (entity.all { it.seat != null }) {
                adapter.players = entity.sortedBy { it.seat }
            } else {
                adapter.players = entity
            }
        })

        randomButton.setOnClickListener {
            viewModel.randomizePlayers()
        }
        clearButton.setOnClickListener {
            viewModel.clearSortOrder()
        }

        doneButton.setOnClickListener {
            viewModel.finishPlayerSort()
        }
    }

    private class Diff(private val oldList: List<NewPlayPlayerEntity>, private val newList: List<NewPlayPlayerEntity>) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val o = oldList[oldItemPosition]
            val n = newList[newItemPosition]
            return o.id == n.id && o.sortOrder == n.sortOrder
        }
    }

    private class PlayersAdapter(private val viewModel: NewPlayViewModel)
        : RecyclerView.Adapter<PlayersAdapter.PlayersViewHolder>() {

        var players: List<NewPlayPlayerEntity> by Delegates.observable(emptyList()) { _, oldValue, newValue ->
            val diffCallback = Diff(oldValue, newValue)
            val diffResult = DiffUtil.calculateDiff(diffCallback)
            diffResult.dispatchUpdatesTo(this)
        }

        override fun getItemCount() = players.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayersViewHolder {
            return PlayersViewHolder(parent.inflate(R.layout.row_new_play_player_sort))
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

                    itemView.setOnClickListener { viewModel.selectStartPlayer(position) }
                }
            }
        }
    }
}