package com.boardgamegeek.ui.dialog

import android.app.Dialog
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
import com.boardgamegeek.ui.widget.GameFamilyRow
import com.boardgamegeek.ui.widget.GameSubtypeRow
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GameRanksDialogFragment : DialogFragment() {
    private var _binding: DialogGameRanksBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<GameViewModel>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogGameRanksBinding.inflate(layoutInflater)
        val builder = requireContext().createThemedBuilder()
            .setView(binding.root)
            .setTitle(R.string.title_ranks_ratings)
        return builder.create()
    }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setTitle(R.string.title_ranks_ratings)

        viewModel.game.observe(viewLifecycleOwner) {
            val voteCount = it?.numberOfRatings ?: 0
            val standardDeviation = it?.standardDeviation ?: 0.0
            binding.votesView.text = requireContext().getQuantityText(R.plurals.ratings_suffix, voteCount, voteCount)
            binding.standardDeviationView.text = requireContext().getText(R.string.standard_deviation_prefix, standardDeviation)
            binding.standardDeviationView.isVisible = voteCount > 0
        }

        viewModel.subtypes.observe(viewLifecycleOwner) {
            binding.unRankedView.isVisible = false
            binding.subtypesView.removeAllViews()
            binding.subtypesView.isVisible = false

            var hasRankedSubtype = false
            var unRankedSubtype = getText(R.string.game)

            it?.forEach { rank ->
                if (rank.isRankValid()) {
                    val row = GameSubtypeRow(requireContext(), rank)
                    binding.subtypesView.addView(row)
                    binding.subtypesView.isVisible = true
                    binding.unRankedView.isVisible = false
                    hasRankedSubtype = true
                } else {
                    unRankedSubtype = rank.describeType(requireContext())
                }
            }
            if (!hasRankedSubtype && unRankedSubtype.isNotEmpty()) {
                binding.unRankedView.text = requireContext().getText(R.string.unranked_prefix, unRankedSubtype)
                binding.unRankedView.isVisible = true
            }
        }

        viewModel.families.observe(viewLifecycleOwner) { list ->
            binding.familiesView.removeAllViews()
            binding.familiesView.isVisible = false
            list?.filter { it.isRankValid() }?.forEach { rank ->
                binding.familiesView.addView(GameFamilyRow(requireContext(), rank))
                binding.familiesView.isVisible = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun launch(host: Fragment) {
            host.showAndSurvive(GameRanksDialogFragment())
        }
    }
}
