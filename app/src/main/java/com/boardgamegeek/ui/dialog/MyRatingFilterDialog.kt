package com.boardgamegeek.ui.dialog

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.filterer.MyRatingFilterer

class MyRatingFilterDialog : RatingFilterDialog<MyRatingFilterer>() {
    override val titleResId = R.string.menu_my_rating

    override fun getType(context: Context) = MyRatingFilterer(context).type

    override fun createTypedFilterer(context: Context): MyRatingFilterer {
        return MyRatingFilterer(context)
    }
}
