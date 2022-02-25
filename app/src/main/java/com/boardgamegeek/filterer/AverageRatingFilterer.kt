package com.boardgamegeek.filterer

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.provider.BggContract.Games

class AverageRatingFilterer(context: Context) : RatingFilterer(context) {
    override val typeResourceId = R.string.collection_filter_type_average_rating

    override val columnName = Games.Columns.STATS_AVERAGE

    override val iconResourceId: Int
        get() = R.drawable.ic_rating // TODO use a half-star

    override fun chipText() = describe(R.string.average_rating_abbr, R.string.unrated_abbr)

    override fun description() = describe(R.string.average_rating, R.string.unrated)

    override fun filter(item: CollectionItemEntity) = filter(item.averageRating)
}
