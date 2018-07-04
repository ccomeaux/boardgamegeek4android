package com.boardgamegeek.ui.dialog

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.boardgamegeek.*
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.viewmodel.GameViewModel
import com.boardgamegeek.ui.widget.GameRankRow
import kotlinx.android.synthetic.main.dialog_game_ranks.*
import org.jetbrains.anko.support.v4.act
import org.jetbrains.anko.support.v4.ctx
import timber.log.Timber

class GameRanksFragment : DialogFragment() {
    private val viewModel: GameViewModel by lazy {
        ViewModelProviders.of(act).get(GameViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_game_ranks, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog.setTitle(R.string.title_ranks_ratings)

        viewModel.game.observe(this, Observer {
            val voteCount = it?.data?.numberOfRatings ?: 0
            val standardDeviation = it?.data?.standardDeviation ?: 0.0
            votesView?.text = ctx.getQuantityText(R.plurals.votes_suffix, voteCount, voteCount)
            if (voteCount == 0) {
                standardDeviationView?.visibility = View.GONE
            } else {
                standardDeviationView?.text = ctx.getText(R.string.standard_deviation_prefix, standardDeviation)
                standardDeviationView?.visibility = View.VISIBLE
            }
        })

        viewModel.ranks.observe(this, Observer { gameRankEntities ->
            subtypesView?.removeAllViews()
            familiesView?.removeAllViews()

            var hasRankedSubtype = false
            var unRankedSubtype = getText(R.string.game)

            if (gameRankEntities?.isNotEmpty() == true) {
                gameRankEntities.forEach { rank ->
                    if (rank.value.isRankValid()) {
                        val row = GameRankRow(ctx, rank.isFamilyType)
                        row.setRank(rank.value)
                        row.setName(rank.name.asRankDescription(ctx, rank.type))
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
                        unRankedSubtype = rank.name.asRankDescription(ctx, rank.type)
                    }
                }
            }
            if (!hasRankedSubtype && unRankedSubtype.isNotEmpty()) {
                unRankedView?.text = ctx.getText(R.string.unranked_prefix, unRankedSubtype)
                unRankedView?.visibility = View.VISIBLE
            }
        })
    }

    companion object {
        fun launch(host: Fragment) {
            val dialog = GameRanksFragment()
            dialog.setStyle(DialogFragment.STYLE_NORMAL, R.style.Theme_bgglight_Dialog)
            host.showAndSurvive(dialog)
        }
    }
}
