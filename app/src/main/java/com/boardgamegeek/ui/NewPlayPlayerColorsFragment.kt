package com.boardgamegeek.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentNewPlayPlayerColorsBinding
import com.boardgamegeek.databinding.RowNewPlayPlayerColorBinding
import com.boardgamegeek.entities.NewPlayPlayer
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.adapter.AutoUpdatableAdapter
import com.boardgamegeek.ui.dialog.NewPlayPlayerColorPickerDialogFragment
import com.boardgamegeek.ui.dialog.TeamPickerDialogFragment
import com.boardgamegeek.ui.viewmodel.NewPlayViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlin.properties.Delegates

@AndroidEntryPoint
class NewPlayPlayerColorsFragment : Fragment() {
    private var _binding: FragmentNewPlayPlayerColorsBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<NewPlayViewModel>()
    private val adapter: PlayersAdapter by lazy { PlayersAdapter(requireActivity(), viewModel) }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentNewPlayPlayerColorsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.adapter = adapter

        viewModel.addedPlayers.observe(viewLifecycleOwner) {
            adapter.players = it
        }
        viewModel.gameColors.observe(viewLifecycleOwner) { colors ->
            colors?.let {
                adapter.useColorPicker = it.all { color -> color.isKnownColor() }
            }
        }

        binding.nextButton.setOnClickListener {
            viewModel.finishPlayerColors()
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.setSubtitle(R.string.title_team_colors)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class PlayersAdapter(private val activity: FragmentActivity, private val viewModel: NewPlayViewModel) :
        RecyclerView.Adapter<PlayersAdapter.PlayersViewHolder>(), AutoUpdatableAdapter {

        var useColorPicker = true
        private var featuredColors = emptyList<String>()
        private var selectedColors = emptyList<String>()

        init {
            viewModel.gameColors.observe(activity) {
                it?.let { featuredColors = it }
            }
            viewModel.selectedColors.observe(activity) {
                selectedColors = it
            }
        }

        var players: List<NewPlayPlayer> by Delegates.observable(emptyList()) { _, oldValue, newValue ->
            autoNotify(oldValue, newValue) { old, new ->
                old.id == new.id
            }
        }

        override fun getItemCount() = players.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayersViewHolder {
            return PlayersViewHolder(parent.inflate(R.layout.row_new_play_player_color))
        }

        override fun onBindViewHolder(holder: PlayersViewHolder, position: Int) {
            holder.bind(players.getOrNull(position))
        }

        inner class PlayersViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val binding = RowNewPlayPlayerColorBinding.bind(itemView)

            fun bind(newPlayPlayer: NewPlayPlayer?) {
                newPlayPlayer?.let { player ->
                    binding.nameView.text = player.name
                    binding.usernameView.setTextOrHide(player.username)

                    if (player.color.isBlank()) {
                        binding.colorView.isInvisible = true
                        binding.teamView.isVisible = false
                        binding.removeTeamView.isInvisible = true
                    } else {
                        val color = player.color.asColorRgb()
                        if (color == Color.TRANSPARENT) {
                            binding.colorView.isInvisible = true
                            binding.teamView.setTextOrHide(player.color)
                            binding.removeTeamView.isVisible = true
                            binding.removeTeamView.setOnClickListener {
                                viewModel.addColorToPlayer(bindingAdapterPosition, "")
                            }
                        } else {
                            binding.colorView.setColorViewValue(color)
                            binding.colorView.setOnClickListener {
                                viewModel.addColorToPlayer(bindingAdapterPosition, "")
                            }
                            binding.colorView.isVisible = true
                            binding.teamView.isVisible = false
                            binding.removeTeamView.isInvisible = true
                        }
                    }

                    binding.colorPickerButton.setImageResource(if (useColorPicker) R.drawable.ic_baseline_color_lens_24 else R.drawable.ic_baseline_group_24)

                    val favoriteColor = player.favoriteColorsForGame.firstOrNull().orEmpty()
                    val favoriteColorRgb = favoriteColor.asColorRgb()
                    if (player.color.isBlank() && favoriteColorRgb != Color.TRANSPARENT) {
                        binding.favoriteColorView.setColorViewValue(favoriteColorRgb)
                        binding.favoriteColorView.setOnClickListener {
                            viewModel.addColorToPlayer(bindingAdapterPosition, favoriteColor)
                        }
                        binding.favoriteColorView.isVisible = true
                    } else {
                        binding.favoriteColorView.isVisible = false
                    }

                    binding.colorPickerButton.setOnClickListener {
                        pickTeamOrColor(player)
                    }
                    itemView.setOnClickListener {
                        pickTeamOrColor(player)
                    }
                }
            }

            private fun pickTeamOrColor(player: NewPlayPlayer) {
                if (useColorPicker) {
                    NewPlayPlayerColorPickerDialogFragment.launch(
                        activity,
                        player.description,
                        featuredColors,
                        player.color,
                        selectedColors,
                        bindingAdapterPosition
                    )
                } else {
                    TeamPickerDialogFragment.launch(activity, bindingAdapterPosition, player.description, player.color)
                }
            }
        }
    }
}
