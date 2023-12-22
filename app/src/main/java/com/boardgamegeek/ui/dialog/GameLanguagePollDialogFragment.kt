package com.boardgamegeek.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentPollBinding
import com.boardgamegeek.extensions.BggColors
import com.boardgamegeek.extensions.showAndSurvive
import com.boardgamegeek.model.GameLanguagePoll
import com.boardgamegeek.ui.viewmodel.GameViewModel
import com.boardgamegeek.ui.widget.IntegerValueFormatter
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment
import com.github.mikephil.charting.components.Legend.LegendVerticalAlignment
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.text.DecimalFormat

@AndroidEntryPoint
class GameLanguagePollDialogFragment : DialogFragment() {
    private var snackBar: Snackbar? = null
    private var _binding: FragmentPollBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<GameViewModel>()

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentPollBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.chartView.apply {
            setDrawEntryLabels(false)
            isRotationEnabled = false
            legend.horizontalAlignment = LegendHorizontalAlignment.LEFT
            legend.verticalAlignment = LegendVerticalAlignment.BOTTOM
            legend.isWordWrapEnabled = true
            description = null
            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry, h: Highlight) {
                    val pe = e as PieEntry?
                    val v = getView()
                    if (pe == null || v == null) {
                        snackBar?.dismiss()
                    } else {
                        val message = resources.getQuantityString(R.plurals.pie_chart_click_description, pe.y.toInt(), FORMAT.format(pe.y), pe.label)
                        if (snackBar == null) snackBar = Snackbar.make(v, message, Snackbar.LENGTH_INDEFINITE)
                        else snackBar?.setText(message)
                        snackBar?.show()
                    }
                }

                override fun onNothingSelected() {
                    snackBar?.dismiss()
                }
            })
        }

        // size the graph to be 80% of the screen width
        binding.chartView.updateLayoutParams {
            width = (resources.displayMetrics.widthPixels * .8).toInt()
        }

        dialog?.setTitle(R.string.language_dependence)
        viewModel.languagePoll.observe(viewLifecycleOwner) {
            it?.let { showLanguageData(it) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showLanguageData(poll: GameLanguagePoll) {
        val totalVoteCount = poll.totalVotes
        if (totalVoteCount > 0) {
            val entries = poll.results.sortedBy { it.level }.map {
                it.level?.let { level ->
                    val resId = when (level) {
                        GameLanguagePoll.Level.NONE -> R.string.language_dependence_level_1
                        GameLanguagePoll.Level.SOME -> R.string.language_dependence_level_2
                        GameLanguagePoll.Level.MODERATE -> R.string.language_dependence_level_3
                        GameLanguagePoll.Level.EXTENSIVE -> R.string.language_dependence_level_4
                        GameLanguagePoll.Level.UNPLAYABLE -> R.string.language_dependence_level_5
                    }
                    PieEntry(it.numberOfVotes.toFloat(), getString(resId))
                }
            }
            val dataSet = PieDataSet(entries.filterNotNull(), "").apply {
                valueFormatter = IntegerValueFormatter(true)
                setColors(*BggColors.fiveStageColors.toIntArray())
            }

            binding.chartView.data = PieData(dataSet)
            binding.chartView.centerText = resources.getQuantityString(R.plurals.votes_suffix, totalVoteCount, totalVoteCount)
            binding.chartView.animateY(1000, Easing.EaseOutCubic)
        }
        binding.progressView.hide()
        binding.scrollView.isVisible = true
    }

    companion object {
        private val FORMAT = DecimalFormat("#0")

        fun launch(host: Fragment) {
            host.showAndSurvive(GameLanguagePollDialogFragment().apply {
                setStyle(STYLE_NORMAL, R.style.Theme_bgglight_Dialog)
            })
        }
    }
}
