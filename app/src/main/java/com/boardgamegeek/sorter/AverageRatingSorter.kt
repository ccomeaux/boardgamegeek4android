package com.boardgamegeek.sorter

import android.content.Context
import android.support.annotation.StringRes

import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract.Collection

import java.text.DecimalFormat

class AverageRatingSorter(context: Context) : RatingSorter(context) {
    private val format = DecimalFormat("0.00")

    override val descriptionId: Int
        @StringRes
        get() = R.string.collection_sort_average_rating

    public override val typeResource: Int
        @StringRes
        get() = R.string.collection_sort_type_average_rating

    override val sortColumn: String
        get() = Collection.STATS_AVERAGE

    override val displayFormat: DecimalFormat
        get() = format
}
