package com.boardgamegeek.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.widget.TextViewCompat
import com.boardgamegeek.R
import com.boardgamegeek.extensions.*
import kotlinx.android.synthetic.main.row_game_rank_subtype.view.*
import java.text.DecimalFormat

@SuppressLint("ViewConstructor")
class GameRankRow(context: Context, isFamily: Boolean) : LinearLayout(context) {
    init {
        LayoutInflater.from(context).inflate(R.layout.row_game_rank_subtype, this)
        if (isFamily) {
            TextViewCompat.setTextAppearance(rankView, R.style.Text)
            TextViewCompat.setTextAppearance(nameView, R.style.Text)
            ratingView?.setTypeface(ratingView?.typeface, Typeface.NORMAL)
        } else {
            TextViewCompat.setTextAppearance(rankView, R.style.Text_Subtitle)
            TextViewCompat.setTextAppearance(nameView, R.style.Text_Subtitle)
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
