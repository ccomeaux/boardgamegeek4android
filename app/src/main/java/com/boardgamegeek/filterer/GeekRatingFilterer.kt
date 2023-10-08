package com.boardgamegeek.filterer

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItem
import com.boardgamegeek.provider.BggContract.Games

class GeekRatingFilterer(context: Context) : RatingFilterer(context) {
    override val typeResourceId = R.string.collection_filter_type_geek_rating

    override val columnName = Games.Columns.STATS_BAYES_AVERAGE

    override val iconResourceId: Int
        get() = R.drawable.ic_baseline_hotel_class_24

    override fun chipText() = describe(R.string.unrated_abbr)

    override fun description() = describe(R.string.unrated, R.string.geek_rating)

    override fun filter(item: CollectionItem) = filter(item.geekRating)
}
