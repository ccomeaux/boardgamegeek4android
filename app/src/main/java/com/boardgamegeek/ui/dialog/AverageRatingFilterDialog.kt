package com.boardgamegeek.ui.dialog

import android.content.Context

import com.boardgamegeek.R
import com.boardgamegeek.filterer.AverageRatingFilterer

class AverageRatingFilterDialog : RatingFilterDialog<AverageRatingFilterer>() {
    override val titleResId = R.string.menu_average_rating

    override fun getType(context: Context) = AverageRatingFilterer(context).type

    override fun createFilterer(context: Context): AverageRatingFilterer {
        return AverageRatingFilterer(context)
    }
}
