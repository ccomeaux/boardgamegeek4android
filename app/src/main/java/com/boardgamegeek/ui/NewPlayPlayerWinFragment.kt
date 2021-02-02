package com.boardgamegeek.ui

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
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
import com.boardgamegeek.entities.NewPlayPlayerEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.dialog.NewPlayerScoreNumberPadDialogFragment
import com.boardgamegeek.ui.viewmodel.NewPlayViewModel
import com.boardgamegeek.util.DialogUtils
import kotlinx.android.synthetic.main.fragment_new_play_player_win.*
import kotlinx.android.synthetic.main.row_new_play_player_win.view.*
import kotlin.properties.Delegates

class NewPlayPlayerWinFragment : Fragment(R.layout.fragment_new_play_player_win) {
    private val viewModel by activityViewModels<NewPlayViewModel>()
    private val adapter: PlayersAdapter by lazy { PlayersAdapter(requireActivity(), viewModel) }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.setSubtitle(R.string.title_winners)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.adapter = adapter

        viewModel.addedPlayers.observe(viewLifecycleOwner, { entity ->
            adapter.players = entity.sortedBy { it.seat }
        })

        nextButton.setOnClickListener {
            viewModel.finishPlayerWin()
        }
    }

    private class PlayersAdapter(private val activity: FragmentActivity, private val viewModel: NewPlayViewModel)
        : RecyclerView.Adapter<PlayersAdapter.PlayersViewHolder>() {

        var players: List<NewPlayPlayerEntity> by Delegates.observable(emptyList()) { _, oldValue, newValue ->
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

        inner class PlayersViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            fun bind(position: Int) {
                val entity = players.getOrNull(position)
                entity?.let { player ->
                    itemView.nameView.text = player.name
                    itemView.usernameView.setTextOrHide(player.username)

                    if (player.color.isBlank()) {
                        itemView.colorView.isInvisible = true
                        itemView.teamView.isVisible = false
                        itemView.seatView.setTextColor(Color.TRANSPARENT.getTextColor())
                    } else {
                        val color = player.color.asColorRgb()
                        if (color == Color.TRANSPARENT) {
                            itemView.colorView.isInvisible = true
                            itemView.teamView.setTextOrHide(player.color)
                        } else {
                            itemView.colorView.setColorViewValue(color)
                            itemView.colorView.isVisible = true
                            itemView.teamView.isVisible = false
                        }
                        itemView.seatView.setTextColor(color.getTextColor())
                    }

                    if (player.seat == null) {
                        itemView.sortView.setTextOrHide(player.sortOrder)
                        itemView.seatView.isInvisible = true
                    } else {
                        itemView.sortView.isVisible = false
                        itemView.seatView.text = player.seat.toString()
                        itemView.seatView.isVisible = true
                    }

                    itemView.scoreView.text = player.getScoreDescription(itemView.context)

                    itemView.scoreButton.setColorFilter(ContextCompat.getColor(itemView.context, R.color.button_under_text), PorterDuff.Mode.SRC_IN)
                    itemView.scoreButton.setSelectableBackgroundBorderless()
                    itemView.scoreButton.setOnClickListener { score(player) }

                    itemView.winCheckBox.isChecked = player.isWin

                    itemView.winCheckBox.setOnCheckedChangeListener { _, isChecked ->
                        viewModel.addWinToPlayer(player.id, isChecked)
                        itemView.winCheckBox.setOnCheckedChangeListener { _, _ -> }
                    }
                }
            }
        }

        private fun score(player: NewPlayPlayerEntity) {
            val fragment = NewPlayerScoreNumberPadDialogFragment.newInstance(
                    player.id,
                    player.score,
                    player.color,
                    player.description)
            DialogUtils.showFragment(activity, fragment, "score_dialog")
        }
    }
}