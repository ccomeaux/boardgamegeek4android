package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.entities.PlayerEntity
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.extensions.inflate
import com.boardgamegeek.extensions.setTextOrHide
import com.boardgamegeek.ui.adapter.AutoUpdatableAdapter
import com.boardgamegeek.ui.viewmodel.PlayersViewModel
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration.SectionCallback
import kotlinx.android.synthetic.main.fragment_players.*
import kotlinx.android.synthetic.main.row_players_player.view.*
import kotlin.properties.Delegates

class PlayersFragment : Fragment() {
    private val viewModel by activityViewModels<PlayersViewModel>()

    private val adapter: PlayersAdapter by lazy {
        PlayersAdapter(viewModel)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_players, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter
        val sectionItemDecoration = RecyclerSectionItemDecoration(
                resources.getDimensionPixelSize(R.dimen.recycler_section_header_height),
                adapter)
        recyclerView.addItemDecoration(sectionItemDecoration)

        viewModel.players.observe(viewLifecycleOwner, Observer {
            adapter.players = it
            progressBar?.hide()
            if (adapter.itemCount == 0) {
                recyclerView.fadeOut()
                emptyContainer.fadeIn()
            } else {
                emptyContainer.fadeOut()
                recyclerView.fadeIn(recyclerView.windowToken != null)
            }
        })
    }

    class PlayersAdapter(val viewModel: PlayersViewModel) : RecyclerView.Adapter<PlayersAdapter.ViewHolder>(), AutoUpdatableAdapter, SectionCallback {
        var players: List<PlayerEntity> by Delegates.observable(emptyList()) { _, oldValue, newValue ->
            autoNotify(oldValue, newValue) { old, new ->
                old.id == new.id
            }
        }

        init {
            setHasStableIds(true)
        }

        override fun getItemId(position: Int): Long {
            return players.getOrNull(position)?.id?.hashCode()?.toLong() ?: RecyclerView.NO_ID
        }

        override fun getItemCount() = players.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(parent.inflate(R.layout.row_players_player))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(players.getOrNull(position))
        }

        override fun isSection(position: Int): Boolean {
            if (position == RecyclerView.NO_POSITION) return false
            if (players.isEmpty()) return false
            if (position == 0) return true
            val thisLetter = viewModel.getSectionHeader(players.getOrNull(position))
            val lastLetter = viewModel.getSectionHeader(players.getOrNull(position - 1))
            return thisLetter != lastLetter
        }

        override fun getSectionHeader(position: Int): CharSequence {
            return when {
                position == RecyclerView.NO_POSITION -> "-"
                players.isEmpty() -> "-"
                else -> viewModel.getSectionHeader(players.getOrNull(position))
            }
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            fun bind(player: PlayerEntity?) {
                player?.let { p ->
                    itemView.nameView.text = p.name
                    itemView.usernameView.setTextOrHide(player.username)
                    itemView.quantityView.setTextOrHide(viewModel.getDisplayText(p))
                    itemView.setOnClickListener {
                        BuddyActivity.start(itemView.context, p.username, p.name)
                    }
                }
            }
        }
    }
}
