package com.boardgamegeek.ui.dialog

import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.viewmodel.GameViewModel
import com.boardgamegeek.ui.widget.GameRankRow
import kotlinx.android.synthetic.main.dialog_game_ranks.*
import timber.log.Timber

class GameRanksFragment : DialogFragment(R.layout.dialog_game_ranks) {
    private val viewModel by activityViewModels<GameViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setTitle(R.string.title_ranks_ratings)

        viewModel.game.observe(this, {
            val voteCount = it?.data?.numberOfRatings ?: 0
            val standardDeviation = it?.data?.standardDeviation ?: 0.0
            votesView?.text = context?.getQuantityText(R.plurals.votes_suffix, voteCount, voteCount)
            if (voteCount == 0) {
                standardDeviationView?.visibility = View.GONE
            } else {
                standardDeviationView?.text = context?.getText(R.string.standard_deviation_prefix, standardDeviation) ?: ""
                standardDeviationView?.visibility = View.VISIBLE
            }
        })

        viewModel.ranks.observe(this, { gameRankEntities ->
            subtypesView?.removeAllViews()
            familiesView?.removeAllViews()

            var hasRankedSubtype = false
            var unRankedSubtype = getText(R.string.game)

            if (gameRankEntities?.isNotEmpty() == true) {
                gameRankEntities.forEach { rank ->
                    if (rank.value.isRankValid()) {
                        val row = GameRankRow(requireContext(), rank.isFamilyType)
                        row.setRank(rank.value)
                        row.setName(rank.name.asRankDescription(requireContext(), rank.type))
                        row.setRatingView(rank.bayesAverage)
                        when {
                            rank.isSubType -> {
                                subtypesView?.addView(row)
                                subtypesView?.visibility = View.VISIBLE
                                unRankedView?.visibility = View.GONE
                                hasRankedSubtype = true
                            }
                            rank.isFamilyType -> {
                                familiesView?.addView(row)
                                familiesView?.visibility = View.VISIBLE
                            }
                            else -> Timber.i("Invalid rank type: ${rank.type}")
                        }
                    } else if (rank.isSubType) {
                        unRankedSubtype = rank.name.asRankDescription(requireContext(), rank.type)
                    }
                }
            }
            if (!hasRankedSubtype && unRankedSubtype.isNotEmpty()) {
                unRankedView?.text = context?.getText(R.string.unranked_prefix, unRankedSubtype) ?: ""
                unRankedView?.visibility = View.VISIBLE
            }
        })
    }

    companion object {
        fun launch(host: Fragment) {
            val dialog = GameRanksFragment()
            dialog.setStyle(STYLE_NORMAL, R.style.Theme_bgglight_Dialog)
            host.showAndSurvive(dialog)
        }
    }
}
