package com.boardgamegeek.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.boardgamegeek.R
import com.boardgamegeek.extensions.setSelectableBackground
import java.text.DecimalFormat

class PlayerStatView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    init {
        LayoutInflater.from(context).inflate(R.layout.widget_player_stat, this)

        orientation = VERTICAL
        val standardPadding = resources.getDimensionPixelSize(R.dimen.padding_standard)
        setPadding(0, standardPadding, 0, standardPadding)
        setSelectableBackground()
    }

    fun showScores(show: Boolean) {
        findViewById<LinearLayout>(R.id.scoresView).isVisible = show
    }

    fun setName(text: CharSequence) {
        findViewById<TextView>(R.id.nameView).text = text
    }

    fun setWinInfo(wins: Int, winnableGames: Int) {
        val winPercentage = when {
            wins >= winnableGames -> 100
            winnableGames > 0 -> (wins.toDouble() / winnableGames * 100).toInt()
            else -> 0
        }
        findViewById<TextView>(R.id.winCountView).text = context.getString(R.string.play_stat_win_percentage, wins, winnableGames, winPercentage)
    }

    fun setWinSkill(skill: Int) {
        findViewById<TextView>(R.id.playCountView).text = skill.toString()
    }

    fun setOverallLowScore(score: Double) {
        findViewById<ScoreGraphView>(R.id.graphView).lowScore = score
    }

    fun setOverallAverageScore(score: Double) {
        findViewById<ScoreGraphView>(R.id.graphView).averageScore = score
    }

    fun setOverallAverageWinScore(score: Double) {
        findViewById<ScoreGraphView>(R.id.graphView).averageWinScore = score
    }

    fun setOverallHighScore(score: Double) {
        findViewById<ScoreGraphView>(R.id.graphView).highScore = score
    }

    fun setLowScore(score: Double) {
        setScore(findViewById(R.id.lowScoreView), score, Integer.MAX_VALUE)
        findViewById<ScoreGraphView>(R.id.graphView).personalLowScore = score
    }

    fun setAverageScore(score: Double) {
        setScore(findViewById(R.id.averageScoreView), score, Integer.MIN_VALUE)
        findViewById<ScoreGraphView>(R.id.graphView).personalAverageScore = score
    }

    fun setAverageWinScore(score: Double) {
        setScore(findViewById(R.id.averageWinScoreView), score, Integer.MIN_VALUE)
        findViewById<ScoreGraphView>(R.id.graphView).personalAverageWinScore = score
    }

    fun setHighScore(score: Double) {
        setScore(findViewById(R.id.highScoreView), score, Integer.MIN_VALUE)
        findViewById<ScoreGraphView>(R.id.graphView).personalHighScore = score
    }

    private fun setScore(textView: TextView, score: Double, invalidScore: Int) {
        textView.text = if (score == invalidScore.toDouble()) "-" else DOUBLE_FORMAT.format(score)
    }

    companion object {
        private val DOUBLE_FORMAT = DecimalFormat("0.##")
    }
}
