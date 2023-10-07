package com.boardgamegeek.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentPlayersBinding
import com.boardgamegeek.databinding.RowPlayersPlayerBinding
import com.boardgamegeek.entities.Player
import com.boardgamegeek.extensions.inflate
import com.boardgamegeek.extensions.setTextOrHide
import com.boardgamegeek.ui.viewmodel.PlayersViewModel
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration.SectionCallback
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PlayersFragment : Fragment() {
    private var _binding: FragmentPlayersBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<PlayersViewModel>()
    private val adapter: PlayersAdapter by lazy { PlayersAdapter(viewModel) }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentPlayersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.addItemDecoration(
            RecyclerSectionItemDecoration(
                resources.getDimensionPixelSize(R.dimen.recycler_section_header_height),
                adapter
            )
        )

        viewModel.players.observe(viewLifecycleOwner) {
            adapter.players = it
            binding.progressBar.hide()
            binding.emptyContainer.isVisible = it.isNullOrEmpty()
            binding.recyclerView.isVisible = !it.isNullOrEmpty()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class PlayersAdapter(val viewModel: PlayersViewModel) : RecyclerView.Adapter<PlayersAdapter.ViewHolder>(), SectionCallback {
        var players: List<Player> = emptyList()
            @SuppressLint("NotifyDataSetChanged")
            set(value) {
                field = value
                notifyDataSetChanged()
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

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val binding = RowPlayersPlayerBinding.bind(itemView)
            fun bind(player: Player?) {
                player?.let {
                    binding.nameView.text = it.name
                    binding.usernameView.setTextOrHide(it.username)
                    binding.quantityView.setTextOrHide(viewModel.getDisplayText(it))
                    itemView.setOnClickListener { _ ->
                        BuddyActivity.start(itemView.context, it.username, it.name)
                    }
                }
            }
        }
    }
}
