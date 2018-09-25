package com.boardgamegeek.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.boardgamegeek.R
import com.boardgamegeek.extensions.setSelectableBackground
import kotlinx.android.synthetic.main.widget_player_stat.view.*
import java.text.DecimalFormat

class PlayerStatView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    init {
        LayoutInflater.from(context).inflate(R.layout.widget_player_stat, this, true)

        orientation = LinearLayout.VERTICAL
        val standardPadding = resources.getDimensionPixelSize(R.dimen.padding_standard)
        setPadding(0, standardPadding, 0, standardPadding)
        setSelectableBackground()
    }

    fun showScores(show: Boolean) {
        scoresView.visibility = if (show) View.VISIBLE else View.GONE
    }

    fun setName(text: CharSequence) {
        nameView.text = text
    }

    fun setWinInfo(wins: Int, winnableGames: Int) {
        val winPercentage = when {
            wins >= winnableGames -> 100
            winnableGames > 0 -> (wins.toDouble() / winnableGames * 100).toInt()
            else -> 0
        }
        winCountView.text = context.getString(R.string.play_stat_win_percentage, wins, winnableGames, winPercentage)
    }

    fun setWinSkill(skill: Int) {
        playCountView.text = skill.toString()
    }

    fun setOverallLowScore(score: Double) {
        graphView.lowScore = score
    }

    fun setOverallAverageScore(score: Double) {
        graphView.averageScore = score
    }

    fun setOverallAverageWinScore(score: Double) {
        graphView.averageWinScore = score
    }

    fun setOverallHighScore(score: Double) {
        graphView.highScore = score
    }

    fun setLowScore(score: Double) {
        setScore(lowScoreView, score, Integer.MAX_VALUE)
        graphView.personalLowScore = score
    }

    fun setAverageScore(score: Double) {
        setScore(averageScoreView, score, Integer.MIN_VALUE)
        graphView.personalAverageScore = score
    }

    fun setAverageWinScore(score: Double) {
        setScore(averageWinScoreView, score, Integer.MIN_VALUE)
        graphView.personalAverageWinScore = score
    }

    fun setHighScore(score: Double) {
        setScore(highScoreView, score, Integer.MIN_VALUE)
        graphView.personalHighScore = score
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
