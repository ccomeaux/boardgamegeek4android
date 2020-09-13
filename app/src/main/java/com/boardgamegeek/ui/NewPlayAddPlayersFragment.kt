package com.boardgamegeek.ui

import android.graphics.Point
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.entities.PlayerEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.adapter.AutoUpdatableAdapter
import com.boardgamegeek.ui.viewmodel.NewPlayViewModel
import com.google.android.material.chip.Chip
import kotlinx.android.synthetic.main.fragment_forums.recyclerView
import kotlinx.android.synthetic.main.fragment_new_play_add_players.*
import kotlinx.android.synthetic.main.row_new_play_add_player.view.*
import kotlin.properties.Delegates

class NewPlayAddPlayersFragment : Fragment(R.layout.fragment_new_play_add_players) {
    private val viewModel by activityViewModels<NewPlayViewModel>()

    private val adapter: PlayersAdapter by lazy {
        PlayersAdapter(viewModel, filterEditText)
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.setSubtitle(R.string.title_players)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val columnWidth = 240
        val size = Point()
        requireActivity().windowManager.defaultDisplay.getSize(size)
        val width = size.x
        recyclerView.layoutManager = GridLayoutManager(context, width / columnWidth)
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter

        filterEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrBlank()) {
                    nextOrAddButton.setImageResource(R.drawable.ic_check_circle)
                } else {
                    nextOrAddButton.setImageResource(R.drawable.ic_add_circle_outline)
                }
                viewModel.filterPlayers(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })

        nextOrAddButton.setOnClickListener {
            if (filterEditText.text?.isNotBlank() == true) {
                viewModel.addPlayer(PlayerEntity(filterEditText.text.toString(), ""))
                filterEditText.setText("")
            } else {
                viewModel.finishAddingPlayers()
            }
        }

        viewModel.availablePlayers.observe(viewLifecycleOwner, {
            adapter.players = it
            recyclerView.fadeIn()
            if (it.isEmpty()) {
                emptyView.setText(if (filterEditText.text.isNullOrBlank()) {
                    R.string.empty_new_play_players
                } else {
                    R.string.empty_new_play_players_filter
                })
                emptyView.fadeIn()
            } else {
                emptyView.fadeOut()
            }
        })
        viewModel.addedPlayers.observe(viewLifecycleOwner, {
            // TODO don't delete and recreate
            chipGroup.removeAllViews()
            it?.let { list ->
                for (player in list) {
                    chipGroup.addView(Chip(context).apply {
                        text = player.description
                        isCloseIconVisible = true
                        if (player.avatarUrl.isBlank()) {
                            setChipIconResource(R.drawable.ic_account_circle_black_24dp)
                            // TODO use non-user's favorite color if available
                            setChipIconTintResource(R.color.dark_blue)
                        } else {
                            loadIcon(player.avatarUrl, R.drawable.ic_account_circle_black_24dp)
                        }
                        isClickable = false
                        setOnCloseIconClickListener {
                            viewModel.removePlayer(player)
                        }
                    })
                }
            }
        })
        viewModel.filterPlayers("")
    }

    private class PlayersAdapter(private val viewModel: NewPlayViewModel, private val filterView: TextView) :
            RecyclerView.Adapter<PlayersAdapter.PlayersViewHolder>(), AutoUpdatableAdapter {
        var players: List<PlayerEntity> by Delegates.observable(emptyList()) { _, oldValue, newValue ->
            autoNotify(oldValue, newValue) { old, new ->
                old.name == new.name && old.username == new.username && old.avatarUrl == new.avatarUrl
            }
        }

        override fun getItemCount() = players.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayersViewHolder {
            return PlayersViewHolder(parent.inflate(R.layout.row_new_play_add_player))
        }

        override fun onBindViewHolder(holder: PlayersViewHolder, position: Int) {
            holder.bind(players.getOrNull(position))
        }

        inner class PlayersViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            fun bind(player: PlayerEntity?) {
                player?.let { p ->
                    itemView.nameView.text = p.name
                    itemView.usernameView.text = p.username
                    itemView.avatarView.loadThumbnail(p.avatarUrl, R.drawable.person_image_empty)
                    itemView.setOnClickListener {
                        viewModel.addPlayer(p)
                        filterView.text = ""
                    }
                }
            }
        }
    }
}
