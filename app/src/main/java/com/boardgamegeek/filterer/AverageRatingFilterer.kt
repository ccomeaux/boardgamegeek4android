package com.boardgamegeek.filterer

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.provider.BggContract.Games

class AverageRatingFilterer(context: Context) : RatingFilterer(context) {
    override val typeResourceId = R.string.collection_filter_type_average_rating

    override val columnName = Games.STATS_AVERAGE

    override fun toShortDescription() = describe(R.string.average_rating_abbr, R.string.unrated_abbr)

    override fun toLongDescription() = describe(R.string.average_rating, R.string.unrated)

    override fun filter(item: CollectionItemEntity) = filter(item.averageRating)
}
