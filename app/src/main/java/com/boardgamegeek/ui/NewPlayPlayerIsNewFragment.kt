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
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentNewPlayPlayerIsNewBinding
import com.boardgamegeek.databinding.RowNewPlayPlayerIsNewBinding
import com.boardgamegeek.entities.NewPlayPlayer
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.adapter.AutoUpdatableAdapter
import com.boardgamegeek.ui.viewmodel.NewPlayViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlin.properties.Delegates

@AndroidEntryPoint
class NewPlayPlayerIsNewFragment : Fragment() {
    private var _binding: FragmentNewPlayPlayerIsNewBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<NewPlayViewModel>()
    private val adapter: PlayersAdapter by lazy { PlayersAdapter(viewModel) }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentNewPlayPlayerIsNewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.adapter = adapter

        viewModel.mightBeNewPlayers.observe(viewLifecycleOwner) { players ->
            adapter.players = players.sortedBy { it.seat }
        }

        binding.nextButton.setOnClickListener {
            viewModel.finishPlayerIsNew()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.setSubtitle(R.string.title_new_players)
    }

    private class PlayersAdapter(private val viewModel: NewPlayViewModel) : RecyclerView.Adapter<PlayersAdapter.PlayersViewHolder>(),
        AutoUpdatableAdapter {
        var players: List<NewPlayPlayer> by Delegates.observable(emptyList()) { _, oldValue, newValue ->
            autoNotify(oldValue, newValue) { old, new ->
                old.id == new.id
            }
        }

        init {
            setHasStableIds(true)
        }

        override fun getItemCount() = players.size

        override fun getItemId(position: Int): Long {
            return players.getOrNull(position)?.id?.hashCode()?.toLong() ?: RecyclerView.NO_ID
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayersViewHolder {
            return PlayersViewHolder(parent.inflate(R.layout.row_new_play_player_is_new))
        }

        override fun onBindViewHolder(holder: PlayersViewHolder, position: Int) {
            holder.bind(position)
        }

        inner class PlayersViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val binding = RowNewPlayPlayerIsNewBinding.bind(itemView)

            fun bind(position: Int) {
                players.getOrNull(position)?.let { player ->
                    binding.nameView.text = player.name
                    binding.usernameView.setTextOrHide(player.username)

                    if (player.color.isBlank()) {
                        binding.colorView.isInvisible = true
                        binding.teamView.isVisible = false
                        binding.seatView.setTextColor(Color.TRANSPARENT.getTextColor())
                    } else {
                        val color = player.color.asColorRgb()
                        if (color == Color.TRANSPARENT) {
                            binding.colorView.isInvisible = true
                            binding.teamView.setTextOrHide(player.color)
                        } else {
                            binding.colorView.setColorViewValue(color)
                            binding.colorView.isVisible = true
                            binding.teamView.isVisible = false
                        }
                        binding.seatView.setTextColor(color.getTextColor())
                    }

                    if (player.seat == null) {
                        binding.sortView.setTextOrHide(player.sortOrder)
                        binding.seatView.isInvisible = true
                    } else {
                        binding.sortView.isVisible = false
                        binding.seatView.text = player.seat.toString()
                        binding.seatView.isVisible = true
                    }

                    binding.isNewCheckBox.isChecked = player.isNew

                    binding.isNewCheckBox.setOnCheckedChangeListener { _, isChecked ->
                        viewModel.addIsNewToPlayer(player.id, isChecked)
                        binding.isNewCheckBox.setOnCheckedChangeListener { _, _ -> }
                    }
                }
            }
        }
    }
}
