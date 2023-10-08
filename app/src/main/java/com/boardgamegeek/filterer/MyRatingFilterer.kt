package com.boardgamegeek.filterer

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.provider.BggContract.Collection

class MyRatingFilterer(context: Context) : RatingFilterer(context) {
    override val typeResourceId = R.string.collection_filter_type_my_rating

    override val columnName = Collection.Columns.RATING

    override val iconResourceId: Int
        get() = R.drawable.ic_baseline_star_rate_24

    override fun chipText() = describe(R.string.unrated_abbr)

    override fun description() = describe(R.string.unrated, R.string.my_rating)

    override fun filter(item: CollectionItem) = filter(item.rating)
}
