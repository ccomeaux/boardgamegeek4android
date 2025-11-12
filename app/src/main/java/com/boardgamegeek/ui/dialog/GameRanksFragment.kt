package com.boardgamegeek.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.boardgamegeek.R
import com.boardgamegeek.databinding.DialogGameRanksBinding
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.viewmodel.GameViewModel
import com.boardgamegeek.ui.widget.GameRankRow
import timber.log.Timber

class GameRanksFragment : DialogFragment() {
    private var _binding: DialogGameRanksBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GameViewModel by lazy {
        ViewModelProvider(this).get(GameViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogGameRanksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.setTitle(R.string.title_ranks_ratings)

        viewModel.game.observe(this, Observer {
            val voteCount = it?.data?.numberOfRatings ?: 0
            val standardDeviation = it?.data?.standardDeviation ?: 0.0
            binding.votesView.text = context?.getQuantityText(R.plurals.votes_suffix, voteCount, voteCount)
            if (voteCount == 0) {
                binding.standardDeviationView.visibility = View.GONE
            } else {
                binding.standardDeviationView.text = context?.getText(R.string.standard_deviation_prefix, standardDeviation) ?: ""
                binding.standardDeviationView.visibility = View.VISIBLE
            }
        })

        viewModel.ranks.observe(this, Observer { gameRankEntities ->
            binding.subtypesView.removeAllViews()
            binding.familiesView.removeAllViews()

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
                                binding.subtypesView.addView(row)
                                binding.subtypesView.visibility = View.VISIBLE
                                binding.unRankedView.visibility = View.GONE
                                hasRankedSubtype = true
                            }
                            rank.isFamilyType -> {
                                binding.familiesView.addView(row)
                                binding.familiesView.visibility = View.VISIBLE
                            }
                            else -> Timber.i("Invalid rank type: ${rank.type}")
                        }
                    } else if (rank.isSubType) {
                        unRankedSubtype = rank.name.asRankDescription(requireContext(), rank.type)
                    }
                }
            }
            if (!hasRankedSubtype && unRankedSubtype.isNotEmpty()) {
                binding.unRankedView.text = context?.getText(R.string.unranked_prefix, unRankedSubtype) ?: ""
                binding.unRankedView.visibility = View.VISIBLE
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun launch(host: Fragment) {
            val dialog = GameRanksFragment()
            dialog.setStyle(STYLE_NORMAL, R.style.Theme_bgglight_Dialog)
            host.showAndSurvive(dialog)
        }
    }
}
