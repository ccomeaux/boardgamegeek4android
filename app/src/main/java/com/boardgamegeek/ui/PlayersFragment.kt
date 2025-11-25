package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentPlayersBinding
import com.boardgamegeek.databinding.RowPlayersPlayerBinding
import com.boardgamegeek.entities.PlayerEntity
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.extensions.setTextOrHide
import com.boardgamegeek.ui.adapter.AutoUpdatableAdapter
import com.boardgamegeek.ui.viewmodel.PlayersViewModel
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration.SectionCallback
import kotlin.properties.Delegates

class PlayersFragment : Fragment() {
    private var _binding: FragmentPlayersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PlayersViewModel by lazy {
        ViewModelProvider(requireActivity()).get(PlayersViewModel::class.java)
    }

    private val adapter: PlayersAdapter by lazy {
        PlayersAdapter(viewModel)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPlayersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter = adapter
        val sectionItemDecoration = RecyclerSectionItemDecoration(
                resources.getDimensionPixelSize(R.dimen.recycler_section_header_height),
                adapter)
        binding.recyclerView.addItemDecoration(sectionItemDecoration)

        viewModel.players.observe(this, Observer {
            adapter.players = it
            binding.progressBar?.hide()
            if (adapter.itemCount == 0) {
                binding.recyclerView.fadeOut()
                binding.emptyContainer.fadeIn()
            } else {
                binding.emptyContainer.fadeOut()
                binding.recyclerView.fadeIn(binding.recyclerView.windowToken != null)
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
            val binding = RowPlayersPlayerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
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

        inner class ViewHolder(private val binding: RowPlayersPlayerBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(player: PlayerEntity?) {
                player?.let { p ->
                    binding.nameView.text = p.name
                    binding.usernameView.setTextOrHide(player.username)
                    binding.quantityView.setTextOrHide(viewModel.getDisplayText(p))
                    binding.root.setOnClickListener {
                        BuddyActivity.start(binding.root.context, p.username, p.name)
                    }
                }
            }
        }
    }
}
