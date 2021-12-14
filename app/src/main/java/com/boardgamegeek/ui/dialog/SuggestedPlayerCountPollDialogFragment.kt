package com.boardgamegeek.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentPollSuggestedPlayerCountBinding
import com.boardgamegeek.extensions.setViewBackground
import com.boardgamegeek.extensions.showAndSurvive
import com.boardgamegeek.ui.viewmodel.GameViewModel
import com.boardgamegeek.ui.widget.PlayerNumberRow

class SuggestedPlayerCountPollDialogFragment : DialogFragment() {
    private var _binding: FragmentPollSuggestedPlayerCountBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<GameViewModel>()

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentPollSuggestedPlayerCountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.setTitle(R.string.suggested_numplayers)

        addKeyRow(R.color.best, R.string.best)
        addKeyRow(R.color.recommended, R.string.recommended)
        addKeyRow(R.color.not_recommended, R.string.not_recommended)

        viewModel.playerPoll.observe(viewLifecycleOwner) {
            it?.let { entity ->
                val totalVoteCount = entity.totalVotes
                binding.totalVoteView.text = resources.getQuantityString(R.plurals.votes_suffix, totalVoteCount, totalVoteCount)

                binding.pollList.isVisible = totalVoteCount > 0
                binding.keyContainer.isVisible = totalVoteCount > 0
                binding.noVotesSwitch.isVisible = totalVoteCount > 0
                if (totalVoteCount > 0) {
                    binding.pollList.removeAllViews()
                    for ((_, playerCount, bestVoteCount, recommendedVoteCount, notRecommendedVoteCount) in entity.results) {
                        val row = PlayerNumberRow(requireContext()).apply {
                            setText(playerCount)
                            setVotes(bestVoteCount, recommendedVoteCount, notRecommendedVoteCount, totalVoteCount)
                            setOnClickListener { view ->
                                binding.pollList.children.forEach { v ->
                                    (v as? PlayerNumberRow)?.clearHighlight()
                                }
                                (view as? PlayerNumberRow)?.let { playerNumberRow ->
                                    playerNumberRow.setHighlight()
                                    binding.keyContainer.children.forEachIndexed { index, view ->
                                        view.findViewById<TextView>(R.id.infoView).text = playerNumberRow.votes[index].toString()
                                    }
                                }
                            }
                        }
                        binding.pollList.addView(row)
                    }

                    binding.noVotesSwitch.setOnClickListener {
                        binding.pollList.children.forEach { row ->
                            (row as? PlayerNumberRow)?.showNoVotes(binding.noVotesSwitch.isChecked)
                        }
                    }
                }

                binding.progressView.hide()
                binding.scrollView.isVisible = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun addKeyRow(@ColorRes colorResId: Int, @StringRes textResId: Int) {
        val row = (LayoutInflater.from(context).inflate(R.layout.row_poll_key, binding.keyContainer, false) as ViewGroup).apply {
            findViewById<TextView>(R.id.textView).setText(textResId)
            findViewById<View>(R.id.colorView).setViewBackground(ContextCompat.getColor(requireContext(), colorResId))
        }
        binding.keyContainer.addView(row)
    }

    companion object {
        fun launch(host: Fragment) {
            val dialog = SuggestedPlayerCountPollDialogFragment()
            dialog.setStyle(STYLE_NORMAL, R.style.Theme_bgglight_Dialog)
            host.showAndSurvive(dialog)
        }
    }
}
