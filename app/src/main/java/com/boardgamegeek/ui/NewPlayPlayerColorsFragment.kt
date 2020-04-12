package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
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
    private val viewModel: NewPlayViewModel by lazy {
        ViewModelProviders.of(requireActivity()).get(NewPlayViewModel::class.java)
    }

    private val adapter: PlayersAdapter by lazy {
        PlayersAdapter(requireActivity(), viewModel)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_new_play_player_colors, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.adapter = adapter

        viewModel.addedPlayers.observe(this, Observer {
            adapter.players = it
        })

        viewModel.gameColors.observe(this, Observer {
            adapter.featuredColors = ArrayList(it ?: emptyList())
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

        var players: List<NewPlayPlayerEntity> by Delegates.observable(emptyList()) { _, oldValue, newValue ->
            autoNotify(oldValue, newValue) { old, new ->
                old.name == new.name && old.username == new.username && old.color == new.color
            }
        }

        var featuredColors: ArrayList<String> = ArrayList(emptyList())

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
                    itemView.colorPickerButton.setOnClickListener {
                        NewPlayPlayerColorPickerDialogFragment.launch(
                                activity,
                                featuredColors,
                                adapterPosition)
                    }
                    itemView.colorView.setOnClickListener {
                        viewModel.addColorToPlayer(adapterPosition, "")
                    }
                }
            }
        }
    }
}
