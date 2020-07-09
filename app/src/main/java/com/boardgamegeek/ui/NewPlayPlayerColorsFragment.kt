package com.boardgamegeek.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.entities.NewPlayPlayerEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.dialog.NewPlayAddTeamColorDialogFragment
import com.boardgamegeek.ui.dialog.NewPlayPlayerColorPickerDialogFragment
import com.boardgamegeek.ui.dialog.TeamPickerDialogFragment
import com.boardgamegeek.ui.viewmodel.NewPlayViewModel
import kotlinx.android.synthetic.main.fragment_new_play_player_colors.*
import kotlinx.android.synthetic.main.row_new_play_player_color.view.*
import kotlin.properties.Delegates

class NewPlayPlayerColorsFragment : Fragment() {
    private val viewModel by activityViewModels<NewPlayViewModel>()
    private val adapter: PlayersAdapter by lazy {
        PlayersAdapter(requireActivity(), viewModel)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_new_play_player_colors, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.adapter = adapter

        viewModel.addedPlayers.observe(viewLifecycleOwner, Observer {
            adapter.players = it
        })
        viewModel.gameColors.observe(viewLifecycleOwner, Observer { colors ->
            adapter.useColorPicker = colors.all { color -> color.isKnownColor() }
        })

        doneButton.setOnClickListener {
            viewModel.finishPlayerColors()
        }
    }

    companion object {
        fun newInstance(): NewPlayPlayerColorsFragment {
            return NewPlayPlayerColorsFragment()
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
            return o.id == n.id && o.color == n.color && o.favoriteColors == n.favoriteColors
        }
    }

    private class PlayersAdapter(private val activity: FragmentActivity, private val viewModel: NewPlayViewModel)
        : RecyclerView.Adapter<PlayersAdapter.PlayersViewHolder>() {

        var useColorPicker = true
        private var featuredColors = emptyList<String>()
        private var selectedColors = emptyList<String>()

        init {
            viewModel.gameColors.observe(activity, Observer {
                featuredColors = it
            })
            viewModel.selectedColors.observe(activity, Observer {
                selectedColors = it
            })
        }

        var players: List<NewPlayPlayerEntity> by Delegates.observable(emptyList()) { _, oldValue, newValue ->
            val diffCallback = Diff(oldValue, newValue)
            val diffResult = DiffUtil.calculateDiff(diffCallback)
            diffResult.dispatchUpdatesTo(this)
        }

        override fun getItemCount() = players.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayersViewHolder {
            return PlayersViewHolder(parent.inflate(R.layout.row_new_play_player_color))
        }

        override fun onBindViewHolder(holder: PlayersViewHolder, position: Int) {
            holder.bind(players.getOrNull(position))
        }

        inner class PlayersViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            fun bind(entity: NewPlayPlayerEntity?) {
                entity?.let { player ->
                    itemView.nameView.text = player.name
                    itemView.usernameView.setTextOrHide(player.username)

                    if (player.color.isBlank()) {
                        itemView.colorView.isInvisible = true
                        itemView.teamView.isVisible = false
                        itemView.removeTeamView.isInvisible = true
                    } else {
                        val color = player.color.asColorRgb()
                        if (color == Color.TRANSPARENT) {
                            itemView.colorView.isInvisible = true
                            itemView.teamView.setTextOrHide(player.color)
                            itemView.removeTeamView.isVisible = true
                            itemView.removeTeamView.setOnClickListener {
                                viewModel.addColorToPlayer(adapterPosition, "")
                            }
                        } else {
                            itemView.colorView.setColorViewValue(color)
                            itemView.colorView.setOnClickListener {
                                viewModel.addColorToPlayer(adapterPosition, "")
                            }
                            itemView.colorView.isVisible = true
                            itemView.teamView.isVisible = false
                            itemView.removeTeamView.isInvisible = true
                        }
                    }
                    if (useColorPicker) {
                        itemView.colorPickerButton.setImageResource(R.drawable.ic_colors)
                    } else {
                        itemView.colorPickerButton.setImageResource(R.drawable.ic_people)
                    }

                    val favoriteColor = player.favoriteColors.firstOrNull() ?: ""
                    val favoriteColorRgb = favoriteColor.asColorRgb()
                    if (player.color.isBlank() && favoriteColorRgb != Color.TRANSPARENT) {
                        itemView.favoriteColorView.setColorViewValue(favoriteColorRgb)
                        itemView.favoriteColorView.setOnClickListener {
                            viewModel.addColorToPlayer(adapterPosition, favoriteColor)
                        }
                        itemView.favoriteColorView.isVisible = true
                    } else {
                        itemView.favoriteColorView.isVisible = false
                    }

                    itemView.colorPickerButton.setOnClickListener {
                        if (useColorPicker) {
                            NewPlayPlayerColorPickerDialogFragment.launch(
                                    activity,
                                    player.description,
                                    featuredColors,
                                    player.color,
                                    selectedColors,
                                    adapterPosition)
                        } else {
                            TeamPickerDialogFragment.launch(activity, adapterPosition, player.description, player.color)
                        }
                    }

                    itemView.addTeamColorPickerButton.setOnClickListener {
                        NewPlayAddTeamColorDialogFragment.newInstance(adapterPosition).show(activity.supportFragmentManager, "")
                    }
                }
            }
        }
    }
}
