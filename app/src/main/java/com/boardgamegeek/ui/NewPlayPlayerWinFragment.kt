package com.boardgamegeek.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentNewPlayPlayerWinBinding
import com.boardgamegeek.databinding.RowNewPlayPlayerWinBinding
import com.boardgamegeek.model.NewPlayPlayer
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.dialog.NewPlayerScoreNumberPadDialogFragment
import com.boardgamegeek.ui.viewmodel.NewPlayViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlin.properties.Delegates

@AndroidEntryPoint
class NewPlayPlayerWinFragment : Fragment() {
    private var _binding: FragmentNewPlayPlayerWinBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<NewPlayViewModel>()
    private val adapter: PlayersAdapter by lazy { PlayersAdapter(requireActivity(), viewModel) }


    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentNewPlayPlayerWinBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.adapter = adapter

        viewModel.addedPlayers.observe(viewLifecycleOwner) { players ->
            adapter.players = players.sortedBy { it.seat }
        }

        binding.nextButton.setOnClickListener {
            viewModel.finishPlayerWin()
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.setSubtitle(R.string.title_winners)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @SuppressLint("NotifyDataSetChanged")
    private class PlayersAdapter(private val activity: FragmentActivity, private val viewModel: NewPlayViewModel) :
        RecyclerView.Adapter<PlayersAdapter.PlayersViewHolder>() {

        var players: List<NewPlayPlayer> by Delegates.observable(emptyList()) { _, _, _ ->
            // using a DiffUtil causes too many crashes
            notifyDataSetChanged()
        }

        init {
            setHasStableIds(true)
        }

        override fun getItemCount() = players.size

        override fun getItemId(position: Int): Long {
            return players.getOrNull(position)?.id?.hashCode()?.toLong() ?: RecyclerView.NO_ID
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayersViewHolder {
            return PlayersViewHolder(parent.inflate(R.layout.row_new_play_player_win))
        }

        override fun onBindViewHolder(holder: PlayersViewHolder, position: Int) {
            holder.bind(position)
        }

        inner class PlayersViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val binding = RowNewPlayPlayerWinBinding.bind(itemView)

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

                    binding.scoreView.text = player.getScoreDescription(itemView.context)

                    binding.scoreButton.setColorFilter(ContextCompat.getColor(itemView.context, R.color.button_under_text), PorterDuff.Mode.SRC_IN)
                    binding.scoreButton.setSelectableBackground(android.R.attr.selectableItemBackgroundBorderless)
                    binding.scoreButton.setOnClickListener { score(player) }

                    binding.winCheckBox.isChecked = player.isWin

                    binding.winCheckBox.setOnCheckedChangeListener { _, isChecked ->
                        viewModel.addWinToPlayer(player.id, isChecked)
                        binding.winCheckBox.setOnCheckedChangeListener { _, _ -> }
                    }
                }
            }
        }

        private fun score(player: NewPlayPlayer) {
            NewPlayerScoreNumberPadDialogFragment.newInstance(
                player.id,
                player.score,
                player.color,
                player.description
            ).show(activity.supportFragmentManager, "score_dialog")
        }
    }
}
