package com.boardgamegeek.filterer

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract.Collection

class GeekRatingFilterer(context: Context) : RatingFilterer(context) {
    override val typeResourceId = R.string.collection_filter_type_geek_rating

    override val columnName = Collection.STATS_BAYES_AVERAGE

    override fun toShortDescription() = describe(R.string.rating, R.string.unrated_abbr)

    override fun toLongDescription() = describe(R.string.geek_rating, R.string.unrated)
}
