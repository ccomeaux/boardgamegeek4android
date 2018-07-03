package com.boardgamegeek.filterer

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract.Collection

class MyRatingFilterer(context: Context) : RatingFilterer(context) {
    override val typeResourceId = R.string.collection_filter_type_my_rating

    override val columnName = Collection.RATING

    override fun getDisplayText() = describe(R.string.my_rating_abbr, R.string.unrated_abbr)

    override fun getDescription() = describe(R.string.my_rating, R.string.unrated)
}
