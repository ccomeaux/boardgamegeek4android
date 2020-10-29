package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes

import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.provider.BggContract.Collection

import java.text.DecimalFormat

class AverageRatingSorter(context: Context) : RatingSorter(context) {
    @StringRes
    public override val typeResId = R.string.collection_sort_type_average_rating

    @StringRes
    override val descriptionResId = R.string.collection_sort_average_rating

    override val sortColumn = Collection.STATS_AVERAGE

    override val displayFormat = DecimalFormat("0.00")

    override fun getRating(item: CollectionItemEntity) = item.averageRating
}
