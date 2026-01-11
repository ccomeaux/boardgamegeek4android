package com.boardgamegeek.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.boardgamegeek.R
import com.boardgamegeek.databinding.RowGameRankSubtypeBinding
import com.boardgamegeek.extensions.*
import java.text.DecimalFormat

@SuppressLint("ViewConstructor")
class GameRankRow(context: Context, isFamily: Boolean) : LinearLayout(context) {
    private val binding = RowGameRankSubtypeBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        if (isFamily) {
            @Suppress("DEPRECATION")
            binding.rankView.setTextAppearance(context, R.style.Text)
            @Suppress("DEPRECATION")
            binding.nameView.setTextAppearance(context, R.style.Text)
            binding.ratingView.setTypeface(binding.ratingView.typeface, Typeface.NORMAL)
        } else {
            @Suppress("DEPRECATION")
            binding.rankView.setTextAppearance(context, R.style.Text_Subtitle)
            @Suppress("DEPRECATION")
            binding.nameView.setTextAppearance(context, R.style.Text_Subtitle)
            binding.ratingView.setTypeface(binding.ratingView.typeface, Typeface.BOLD)
        }
    }

    fun setRank(rank: Int) {
        if (rank.isRankValid()) {
            binding.rankView.text = context.getString(R.string.rank_prefix, rank)
            binding.rankView.visibility = View.VISIBLE
        } else {
            binding.rankView.visibility = View.INVISIBLE
        }
    }

    fun setName(name: CharSequence) {
        binding.nameView.text = name
    }

    fun setRatingView(rating: Double) {
        binding.ratingView.text = rating.asScore(context, format = AVERAGE_RATING_FORMAT)
        binding.ratingView.setTextViewBackground(rating.toColor(ratingColors))
    }

    companion object {
        private val AVERAGE_RATING_FORMAT = DecimalFormat("#0.000")
    }
}
