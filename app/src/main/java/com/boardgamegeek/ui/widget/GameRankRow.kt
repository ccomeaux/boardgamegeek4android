package com.boardgamegeek.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.boardgamegeek.*
import kotlinx.android.synthetic.main.row_game_rank_subtype.view.*
import java.text.DecimalFormat

@SuppressLint("ViewConstructor")
class GameRankRow(context: Context, isFamily: Boolean) : LinearLayout(context) {
    init {
        LayoutInflater.from(context).inflate(R.layout.row_game_rank_subtype, this)
        if (isFamily) {
            @Suppress("DEPRECATION")
            rankView?.setTextAppearance(context, R.style.Text)
            @Suppress("DEPRECATION")
            nameView?.setTextAppearance(context, R.style.Text)
            ratingView?.setTypeface(ratingView?.typeface, Typeface.NORMAL)
        } else {
            @Suppress("DEPRECATION")
            rankView?.setTextAppearance(context, R.style.Text_Subtitle)
            @Suppress("DEPRECATION")
            nameView?.setTextAppearance(context, R.style.Text_Subtitle)
            ratingView?.setTypeface(ratingView?.typeface, Typeface.BOLD)
        }
    }

    fun setRank(rank: Int) {
        if (rank.isRankValid()) {
            rankView?.text = context.getString(R.string.rank_prefix, rank)
            rankView?.visibility = View.VISIBLE
        } else {
            rankView?.visibility = View.INVISIBLE
        }
    }

    fun setName(name: CharSequence) {
        nameView?.text = name
    }

    fun setRatingView(rating: Double) {
        ratingView?.text = rating.asScore(context, format = AVERAGE_RATING_FORMAT)
        ratingView.setTextViewBackground(rating.toColor(ratingColors))
    }

    companion object {
        private val AVERAGE_RATING_FORMAT = DecimalFormat("#0.000")
    }
}
