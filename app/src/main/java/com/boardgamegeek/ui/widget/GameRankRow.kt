package com.boardgamegeek.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import com.boardgamegeek.R
import com.boardgamegeek.entities.GameRank
import com.boardgamegeek.extensions.*
import java.text.DecimalFormat

@SuppressLint("ViewConstructor")
class GameRankRow(context: Context, rank: GameRank) : LinearLayout(context) {
    init {
        val isFamily = rank.type == GameRank.RankType.Family
        LayoutInflater.from(context).inflate(R.layout.row_game_rank_subtype, this)
        TextViewCompat.setTextAppearance(findViewById(R.id.rankView), if (isFamily) R.style.TextAppearance_Basic else R.style.TextAppearance_Subtitle)
        TextViewCompat.setTextAppearance(findViewById(R.id.nameView), if (isFamily) R.style.TextAppearance_Basic else R.style.TextAppearance_Subtitle)
        findViewById<TextView>(R.id.ratingView).apply { setTypeface(typeface, if (isFamily) Typeface.NORMAL else Typeface.BOLD) }

        findViewById<TextView>(R.id.rankView).apply {
            if (rank.isRankValid()) {
                text = context.getString(R.string.rank_prefix, rank.value)
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
