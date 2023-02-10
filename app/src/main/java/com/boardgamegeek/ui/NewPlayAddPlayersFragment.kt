package com.boardgamegeek.ui

import android.content.res.ColorStateList
import android.graphics.BlendMode
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentNewPlayAddPlayersBinding
import com.boardgamegeek.databinding.RowNewPlayAddPlayerBinding
import com.boardgamegeek.entities.PlayerEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.adapter.AutoUpdatableAdapter
import com.boardgamegeek.ui.viewmodel.NewPlayViewModel
import com.google.android.material.chip.Chip
import kotlin.properties.Delegates

class NewPlayAddPlayersFragment : Fragment() {
    private var _binding: FragmentNewPlayAddPlayersBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<NewPlayViewModel>()
    private val adapter: PlayersAdapter by lazy { PlayersAdapter(viewModel, binding.filterEditText) }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentNewPlayAddPlayersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val columnWidth = 240
        val width = requireActivity().calculateScreenWidth()
        binding.recyclerView.layoutManager = GridLayoutManager(context, width / columnWidth)
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter = adapter

        binding.filterEditText.doAfterTextChanged { s ->
            if (s.isNullOrBlank()) {
                binding.nextOrAddButton.setImageResource(R.drawable.ic_baseline_check_circle_24)
            } else {
                binding.nextOrAddButton.setImageResource(R.drawable.ic_baseline_add_circle_outline_24)
            }
            viewModel.filterPlayers(s.toString())
        }

        binding.nextOrAddButton.setOnClickListener {
            if (binding.filterEditText.text?.isNotBlank() == true) {
                viewModel.addPlayer(PlayerEntity(binding.filterEditText.text.toString(), ""))
                binding.filterEditText.setText("")
            } else {
                viewModel.finishAddingPlayers()
            }
        }

        viewModel.availablePlayers.observe(viewLifecycleOwner) {
            adapter.players = it
            binding.recyclerView.isVisible = true
            if (it.isEmpty()) {
                binding.emptyView.setText(
                    if (binding.filterEditText.text.isNullOrBlank()) {
                        R.string.empty_new_play_players
                    } else {
                        R.string.empty_new_play_players_filter
                    }
                )
                binding.emptyView.isVisible = true
            } else {
                binding.emptyView.isVisible = false
            }
        }
        viewModel.addedPlayers.observe(viewLifecycleOwner) {
            it?.let { playerList ->
                for (player in playerList) {
                    findOrCreateChip(player.id).apply {
                        text = player.description
                        isCloseIconVisible = true
                        if (player.avatarUrl.isBlank()) {
                            setChipIconResource(R.drawable.ic_baseline_account_circle_24)
                            if (player.favoriteColor?.isNotBlank() == true) {
                                chipIconTint = ColorStateList.valueOf(player.favoriteColor.asColorRgb())
                            }
                        } else {
                            loadIcon(player.avatarUrl, R.drawable.ic_baseline_account_circle_24)
                        }
                        isClickable = false
                        setOnCloseIconClickListener {
                            viewModel.removePlayer(player)
                        }
                    }
                }
                val usedTags = playerList.map { player -> player.id }
                binding.chipGroup.children.forEach { chip ->
                    if (!usedTags.contains(chip.tag)) binding.chipGroup.removeView(chip)
                }
            }
        }
        viewModel.filterPlayers("")
    }

    private fun findOrCreateChip(playerId: String): Chip {
        return binding.chipGroup.findViewWithTag(playerId) ?: Chip(context, null, R.style.Widget_MaterialComponents_Chip_Entry).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            tag = playerId
            binding.chipGroup.addView(this)
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.setSubtitle(R.string.title_players)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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

        inner class PlayersViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val binding = RowNewPlayAddPlayerBinding.bind(itemView)

            fun bind(player: PlayerEntity?) {
                player?.let { p ->
                    binding.nameView.text = p.name
                    binding.usernameView.text = p.username
                    binding.avatarView.loadThumbnail(p.avatarUrl, R.drawable.person_image_empty, object : ImageLoadCallback {
                        override fun onSuccessfulImageLoad(palette: Palette?) {
                        }

                        override fun onFailedImageLoad() {
                            tintPlayer(p)
                        }
                    })
                    if (p.avatarUrl.isBlank() && p.favoriteColor != null) {
                        tintPlayer(p)
                    }
                    itemView.setOnClickListener {
                        viewModel.addPlayer(p)
                        filterView.text = ""
                    }
                }
            }

            private fun tintPlayer(p: PlayerEntity) {
                binding.avatarView.imageTintBlendMode = BlendMode.COLOR_BURN
                binding.avatarView.imageTintList = ColorStateList.valueOf(p.favoriteColor ?: Color.TRANSPARENT)
            }
        }
    }
}
