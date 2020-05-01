package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.boardgamegeek.R
import com.boardgamegeek.entities.GamePollEntity
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fiveStageColors
import com.boardgamegeek.extensions.showAndSurvive
import com.boardgamegeek.extensions.twelveStageColors
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
import kotlinx.android.synthetic.main.fragment_poll.*
import timber.log.Timber
import java.text.DecimalFormat
import java.util.*

class PollFragment : DialogFragment() {
    private var pollType = UNKNOWN
    private var snackBar: Snackbar? = null

    val viewModel by activityViewModels<GameViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pollType = arguments?.getInt(KEY_TYPE, UNKNOWN) ?: UNKNOWN
        if (pollType != LANGUAGE_DEPENDENCE && pollType != SUGGESTED_PLAYER_AGE) {
            Timber.w("Unknown type of $pollType")
            dismiss()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_poll, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        chartView?.setDrawEntryLabels(false)
        chartView?.isRotationEnabled = false
        val legend = chartView?.legend
        legend?.horizontalAlignment = LegendHorizontalAlignment.LEFT
        legend?.verticalAlignment = LegendVerticalAlignment.BOTTOM
        legend?.isWordWrapEnabled = true
        chartView?.description = null
        chartView?.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry, h: Highlight) {
                val pe = e as PieEntry?
                val v = getView()
                if (pe == null || chartView == null || v == null) {
                    snackBar?.dismiss()
                    return
                }

                val message = resources.getQuantityString(R.plurals.pie_chart_click_description, pe.y.toInt(), FORMAT.format(pe.y), pe.label)
                if (snackBar == null) snackBar = Snackbar.make(v, message, Snackbar.LENGTH_INDEFINITE)
                else snackBar?.setText(message)
                snackBar?.show()
            }

            override fun onNothingSelected() {
                snackBar?.dismiss()
            }
        })

        // size the graph to be 80% of the screen width
        val display = this.resources.displayMetrics
        val lp = chartView?.layoutParams
        lp?.width = (display.widthPixels * .8).toInt()
        chartView?.layoutParams = lp
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        when (pollType) {
            LANGUAGE_DEPENDENCE -> {
                dialog?.setTitle(R.string.language_dependence)
                viewModel.languagePoll.observe(this, Observer { showData(it, fiveStageColors) })
            }
            SUGGESTED_PLAYER_AGE -> {
                dialog?.setTitle(R.string.suggested_playerage)
                viewModel.agePoll.observe(this, Observer { showData(it, twelveStageColors) })
            }
        }
    }

    private fun showData(gamePollEntity: GamePollEntity?, chartColors: IntArray) {
        val totalVoteCount = gamePollEntity?.totalVotes ?: 0
        if (totalVoteCount > 0) {
            val entries = ArrayList<PieEntry>()
            for ((_, value, numberOfVotes) in gamePollEntity!!.results) {
                entries.add(PieEntry(numberOfVotes.toFloat(), value))
            }
            val dataSet = PieDataSet(entries, "")
            dataSet.valueFormatter = IntegerValueFormatter(true)
            dataSet.setColors(*chartColors)

            val data = PieData(dataSet)
            chartView?.data = data
            chartView?.centerText = resources.getQuantityString(R.plurals.votes_suffix, totalVoteCount, totalVoteCount)

            chartView?.animateY(1000, Easing.EaseOutCubic)
        }
        progressView?.hide()
        scrollView?.fadeIn()
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
            val arguments = Bundle(1)
            arguments.putInt(KEY_TYPE, type)
            val dialog = PollFragment()
            dialog.arguments = arguments
            dialog.setStyle(STYLE_NORMAL, R.style.Theme_bgglight_Dialog)
            host.showAndSurvive(dialog)
        }
    }
}
