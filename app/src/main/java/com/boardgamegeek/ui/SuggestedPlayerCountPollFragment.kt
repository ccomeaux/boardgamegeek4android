package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.boardgamegeek.R
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.setViewBackground
import com.boardgamegeek.databinding.FragmentPollSuggestedPlayerCountBinding
import com.boardgamegeek.extensions.showAndSurvive
import com.boardgamegeek.ui.viewmodel.GameViewModel
import com.boardgamegeek.ui.widget.PlayerNumberRow

class SuggestedPlayerCountPollFragment : DialogFragment() {
    private var _binding: FragmentPollSuggestedPlayerCountBinding? = null
    private val binding get() = _binding!!
    val viewModel: GameViewModel by lazy {
        ViewModelProvider(requireActivity()).get(GameViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPollSuggestedPlayerCountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        dialog?.setTitle(R.string.suggested_numplayers)

        addKeyRow(R.color.best, R.string.best)
        addKeyRow(R.color.recommended, R.string.recommended)
        addKeyRow(R.color.not_recommended, R.string.not_recommended)

        viewModel.playerPoll.observe(this, Observer { entity ->
            val totalVoteCount = entity?.totalVotes ?: 0
            binding.totalVoteView.text = resources.getQuantityString(R.plurals.votes_suffix, totalVoteCount, totalVoteCount)

            binding.pollList.visibility = if (totalVoteCount == 0) View.GONE else View.VISIBLE
            binding.keyContainer.visibility = if (totalVoteCount == 0) View.GONE else View.VISIBLE
            binding.noVotesSwitch.visibility = if (totalVoteCount == 0) View.GONE else View.VISIBLE
            if (totalVoteCount > 0) {
                binding.pollList.removeAllViews()
                for ((_, playerCount, bestVoteCount, recommendedVoteCount, notRecommendedVoteCount) in entity!!.results) {
                    val row = PlayerNumberRow(requireContext())
                    row.setText(playerCount)
                    row.setVotes(bestVoteCount, recommendedVoteCount, notRecommendedVoteCount, totalVoteCount)
                    row.setOnClickListener { v ->
                        for (i in 0 until binding.pollList.childCount) {
                            (binding.pollList.getChildAt(i) as PlayerNumberRow).clearHighlight()
                        }
                        val playerNumberRow = v as PlayerNumberRow
                        playerNumberRow.setHighlight()

                        val voteCount = playerNumberRow.votes
                        for (i in 0 until binding.keyContainer.childCount) {
                            binding.keyContainer.getChildAt(i).findViewById<TextView>(R.id.infoView).text = voteCount[i].toString()
                        }
                    }
                    binding.pollList.addView(row)
                }

                binding.noVotesSwitch.setOnClickListener {
                    for (i in 0 until binding.pollList.childCount) {
                        val row = binding.pollList.getChildAt(i) as PlayerNumberRow
                        row.showNoVotes(binding.noVotesSwitch.isChecked)
                    }
                }
            }

            binding.progressView.hide()
            binding.scrollView.fadeIn()
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun addKeyRow(@ColorRes colorResId: Int, @StringRes textResId: Int) {
        val v = LayoutInflater.from(context).inflate(R.layout.row_poll_key, binding.keyContainer, false) as ViewGroup
        v.findViewById<TextView>(R.id.textView).setText(textResId)
        v.findViewById<View>(R.id.colorView).setViewBackground(ContextCompat.getColor(requireContext(), colorResId))
        binding.keyContainer.addView(v)
    }

    companion object {
        fun launch(host: Fragment) {
            val dialog = SuggestedPlayerCountPollFragment()
            dialog.setStyle(STYLE_NORMAL, R.style.Theme_bgglight_Dialog)
            host.showAndSurvive(dialog)
        }
    }
}
