package com.boardgamegeek.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.databinding.DialogGameRanksBinding
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.viewmodel.GameViewModel
import com.boardgamegeek.ui.widget.GameRankRow
import timber.log.Timber

class GameRanksFragment : DialogFragment() {
    private var _binding: DialogGameRanksBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<GameViewModel>()

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = DialogGameRanksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setTitle(R.string.title_ranks_ratings)

        viewModel.game.observe(viewLifecycleOwner) {
            val voteCount = it?.data?.numberOfRatings ?: 0
            val standardDeviation = it?.data?.standardDeviation ?: 0.0
            binding.votesView.text = requireContext().getQuantityText(R.plurals.votes_suffix, voteCount, voteCount)
            binding.standardDeviationView.text = requireContext().getText(R.string.standard_deviation_prefix, standardDeviation)
            binding.standardDeviationView.isVisible = voteCount > 0
        }

        viewModel.ranks.observe(viewLifecycleOwner) { gameRankEntities ->
            binding.unRankedView.isVisible = false
            binding.subtypesView.removeAllViews()
            binding.subtypesView.isVisible = false
            binding.familiesView.removeAllViews()
            binding.familiesView.isVisible = false

            var hasRankedSubtype = false
            var unRankedSubtype = getText(R.string.game)

            gameRankEntities?.forEach { rank ->
                if (rank.value.isRankValid()) {
                    val row = GameRankRow(requireContext(), rank.isFamilyType).apply {
                        setRank(rank.value)
                        setName(rank.name.asRankDescription(requireContext(), rank.type))
                        setRatingView(rank.bayesAverage)
                    }
                    when {
                        rank.isSubType -> {
                            binding.subtypesView.addView(row)
                            binding.subtypesView.isVisible = true
                            binding.unRankedView.isVisible = false
                            hasRankedSubtype = true
                        }
                        rank.isFamilyType -> {
                            binding.familiesView.addView(row)
                            binding.familiesView.isVisible = true
                        }
                        else -> Timber.i("Invalid rank type: ${rank.type}")
                    }
                } else if (rank.isSubType) {
                    unRankedSubtype = rank.name.asRankDescription(requireContext(), rank.type)
                }
            }
            if (!hasRankedSubtype && unRankedSubtype.isNotEmpty()) {
                binding.unRankedView.text = requireContext().getText(R.string.unranked_prefix, unRankedSubtype)
                binding.unRankedView.isVisible = true
            }
        }
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
