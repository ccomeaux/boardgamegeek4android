package com.boardgamegeek.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.boardgamegeek.R
import com.boardgamegeek.model.GameSubtype
import com.boardgamegeek.extensions.*
import java.text.DecimalFormat

@SuppressLint("ViewConstructor")
class GameSubtypeRow(context: Context, rank: GameSubtype) : LinearLayout(context) {
    init {
        LayoutInflater.from(context).inflate(R.layout.row_game_subtype, this)

        findViewById<TextView>(R.id.rankView).apply {
            if (rank.isRankValid()) {
                text = context.getString(R.string.rank_prefix, rank.rank)
                isVisible = true
            } else {
                isVisible = false
            }
        }

        findViewById<TextView>(R.id.nameView).text = rank.describeType(context)
        setRatingView(rank.bayesAverage)
    }

    private fun setRatingView(rating: Double) {
        findViewById<TextView>(R.id.ratingView).apply {
            text = rating.asScore(context, format = AVERAGE_RATING_FORMAT)
            setTextViewBackground(rating.toColor(BggColors.ratingColors))
        }
    }

    companion object {
        private val AVERAGE_RATING_FORMAT = DecimalFormat("#0.000")
    }
}
