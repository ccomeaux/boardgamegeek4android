package com.boardgamegeek.ui.dialog

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.filterer.GeekRatingFilterer

class GeekRatingFilterDialog : RatingFilterDialog<GeekRatingFilterer>() {
    override val titleResId = R.string.menu_geek_rating

    override fun getType(context: Context) = GeekRatingFilterer(context).type

    override fun createTypedFilterer(context: Context): GeekRatingFilterer {
        return GeekRatingFilterer(context)
    }
}
