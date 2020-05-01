package com.boardgamegeek.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.entities.NewPlayPlayerEntity
import com.boardgamegeek.extensions.asColorRgb
import com.boardgamegeek.extensions.inflate
import com.boardgamegeek.extensions.setColorViewValue
import com.boardgamegeek.ui.adapter.AutoUpdatableAdapter
import com.boardgamegeek.ui.dialog.NewPlayPlayerColorPickerDialogFragment
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

        doneButton.setOnClickListener {
            viewModel.finishPlayerColors()
        }
    }

    companion object {
        fun newInstance(): NewPlayPlayerColorsFragment {
            return NewPlayPlayerColorsFragment()
        }
    }

    private class PlayersAdapter(private val activity: FragmentActivity, private val viewModel: NewPlayViewModel)
        : RecyclerView.Adapter<PlayersAdapter.PlayersViewHolder>(), AutoUpdatableAdapter {

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
            autoNotify(oldValue, newValue) { old, new ->
                old.name == new.name &&
                        old.username == new.username &&
                        old.avatarUrl == new.avatarUrl &&
                        old.favoriteColors == new.favoriteColors
            }
        }

        override fun getItemCount() = players.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayersViewHolder {
            return PlayersViewHolder(parent.inflate(R.layout.row_new_play_player_color))
        }

        override fun onBindViewHolder(holder: PlayersViewHolder, position: Int) {
            holder.bind(players.getOrNull(position))
        }

        inner class PlayersViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            fun bind(player: NewPlayPlayerEntity?) {
                val color = player?.color.asColorRgb()
                player?.let { p ->
                    itemView.nameView.text = p.name
                    itemView.usernameView.text = p.username

                    itemView.colorView.setColorViewValue(color)
                    itemView.colorView.setOnClickListener {
                        viewModel.addColorToPlayer(adapterPosition, "")
                    }

                    val favoriteColor = p.favoriteColors.firstOrNull() ?: ""
                    val favoriteColorRgb = favoriteColor.asColorRgb()
                    if (p.color.isBlank() && favoriteColorRgb != Color.TRANSPARENT) {
                        itemView.favoriteColorView.setColorViewValue(favoriteColorRgb)
                        itemView.favoriteColorView.setOnClickListener {
                            viewModel.addColorToPlayer(adapterPosition, favoriteColor)
                        }
                        itemView.favoriteColorView.isVisible = true
                    } else {
                        itemView.favoriteColorView.isVisible = false
                    }

                    itemView.colorPickerButton.setOnClickListener {
                        NewPlayPlayerColorPickerDialogFragment.launch(
                                activity,
                                p.description,
                                featuredColors,
                                p.color,
                                selectedColors,
                                adapterPosition)
                    }
                }
            }
        }
    }
}
