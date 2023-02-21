package com.boardgamegeek.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentPollBinding
import com.boardgamegeek.entities.GamePollEntity
import com.boardgamegeek.extensions.BggColors
import com.boardgamegeek.extensions.showAndSurvive
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
import timber.log.Timber
import java.text.DecimalFormat

@AndroidEntryPoint
class GamePollDialogFragment : DialogFragment() {
    private var pollType = UNKNOWN
    private var snackBar: Snackbar? = null
    private var _binding: FragmentPollBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<GameViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pollType = arguments?.getInt(KEY_TYPE, UNKNOWN) ?: UNKNOWN
        if (pollType != LANGUAGE_DEPENDENCE && pollType != SUGGESTED_PLAYER_AGE) {
            Timber.w("Unknown type of $pollType")
            dismiss()
        }
    }

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

        when (pollType) {
            LANGUAGE_DEPENDENCE -> {
                dialog?.setTitle(R.string.language_dependence)
                viewModel.languagePoll.observe(viewLifecycleOwner) {
                    it?.let { showData(it, BggColors.fiveStageColors) }
                }
            }
            SUGGESTED_PLAYER_AGE -> {
                dialog?.setTitle(R.string.suggested_playerage)
                viewModel.agePoll.observe(viewLifecycleOwner) {
                    it?.let { showData(it, BggColors.twelveStageColors) }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showData(gamePollEntity: GamePollEntity, chartColors: List<Int>) {
        val totalVoteCount = gamePollEntity.totalVotes
        if (totalVoteCount > 0) {
            val entries = mutableListOf<PieEntry>()
            for ((_, value, numberOfVotes) in gamePollEntity.results) {
                entries += PieEntry(numberOfVotes.toFloat(), value)
            }
            val dataSet = PieDataSet(entries, "").apply {
                valueFormatter = IntegerValueFormatter(true)
                setColors(*chartColors.toIntArray())
            }

            binding.chartView.data = PieData(dataSet)
            binding.chartView.centerText = resources.getQuantityString(R.plurals.votes_suffix, totalVoteCount, totalVoteCount)
            binding.chartView.animateY(1000, Easing.EaseOutCubic)
        }
        binding.progressView.hide()
        binding.scrollView.isVisible = true
    }

    companion object {
        private const val KEY_TYPE = "TYPE"
        private const val UNKNOWN = 0
        private const val LANGUAGE_DEPENDENCE = 1
        private const val SUGGESTED_PLAYER_AGE = 2
        private val FORMAT = DecimalFormat("#0")

        fun launchLanguageDependence(host: Fragment) {
            launch(host, LANGUAGE_DEPENDENCE)
        }

        fun launchSuggestedPlayerAge(host: Fragment) {
            launch(host, SUGGESTED_PLAYER_AGE)
        }

        private fun launch(host: Fragment, type: Int) {
            host.showAndSurvive(GamePollDialogFragment().apply {
                arguments = bundleOf(KEY_TYPE to type)
                setStyle(STYLE_NORMAL, R.style.Theme_bgglight_Dialog)
            })
        }
    }
}
