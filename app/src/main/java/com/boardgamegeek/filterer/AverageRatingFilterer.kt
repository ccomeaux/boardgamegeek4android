package com.boardgamegeek.filterer

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.provider.BggContract.Games

class AverageRatingFilterer(context: Context) : RatingFilterer(context) {
    override val typeResourceId = R.string.collection_filter_type_average_rating

    override val columnName = Games.Columns.STATS_AVERAGE

    override val iconResourceId: Int
        get() = R.drawable.ic_baseline_star_half_24

    override fun chipText() = describe(R.string.unrated_abbr)

    override fun description() = describe(R.string.unrated, R.string.average_rating)

    override fun filter(item: CollectionItem) = filter(item.averageRating)
}
