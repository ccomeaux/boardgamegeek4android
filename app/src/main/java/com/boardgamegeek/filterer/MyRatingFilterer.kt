package com.boardgamegeek.filterer

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.provider.BggContract.Collection

class MyRatingFilterer(context: Context) : RatingFilterer(context) {
    override val typeResourceId = R.string.collection_filter_type_my_rating

    override val columnName = Collection.Columns.RATING

    override val iconResourceId: Int
        get() = R.drawable.ic_rating

    override fun chipText() = describe(R.string.my_rating_abbr, R.string.unrated_abbr)

    override fun description() = describe(R.string.my_rating, R.string.unrated)

    override fun filter(item: CollectionItemEntity) = filter(item.rating)
}
