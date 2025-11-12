package com.boardgamegeek.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.boardgamegeek.R
import com.boardgamegeek.databinding.WidgetPlayerStatBinding
import com.boardgamegeek.extensions.setSelectableBackground
import java.text.DecimalFormat

class PlayerStatView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private val binding = WidgetPlayerStatBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        orientation = LinearLayout.VERTICAL
        val standardPadding = resources.getDimensionPixelSize(R.dimen.padding_standard)
        setPadding(0, standardPadding, 0, standardPadding)
        setSelectableBackground()
    }

    fun showScores(show: Boolean) {
        binding.scoresView.visibility = if (show) View.VISIBLE else View.GONE
    }

    fun setName(text: CharSequence) {
        binding.nameView.text = text
    }

    fun setWinInfo(wins: Int, winnableGames: Int) {
        val winPercentage = when {
            wins >= winnableGames -> 100
            winnableGames > 0 -> (wins.toDouble() / winnableGames * 100).toInt()
            else -> 0
        }
        binding.winCountView.text = context.getString(R.string.play_stat_win_percentage, wins, winnableGames, winPercentage)
    }

    fun setWinSkill(skill: Int) {
        binding.playCountView.text = skill.toString()
    }

    fun setOverallLowScore(score: Double) {
        binding.graphView.lowScore = score
    }

    fun setOverallAverageScore(score: Double) {
        binding.graphView.averageScore = score
    }

    fun setOverallAverageWinScore(score: Double) {
        binding.graphView.averageWinScore = score
    }

    fun setOverallHighScore(score: Double) {
        binding.graphView.highScore = score
    }

    fun setLowScore(score: Double) {
        setScore(binding.lowScoreView, score, Integer.MAX_VALUE)
        binding.graphView.personalLowScore = score
    }

    fun setAverageScore(score: Double) {
        setScore(binding.averageScoreView, score, Integer.MIN_VALUE)
        binding.graphView.personalAverageScore = score
    }

    fun setAverageWinScore(score: Double) {
        setScore(binding.averageWinScoreView, score, Integer.MIN_VALUE)
        binding.graphView.personalAverageWinScore = score
    }

    fun setHighScore(score: Double) {
        setScore(binding.highScoreView, score, Integer.MIN_VALUE)
        binding.graphView.personalHighScore = score
    }

    private fun setScore(textView: TextView, score: Double, invalidScore: Int) {
        textView.text = if (score == invalidScore.toDouble()) {
            "-"
        } else {
            DOUBLE_FORMAT.format(score)
        }
    }

    companion object {
        private val DOUBLE_FORMAT = DecimalFormat("0.##")
    }
}
