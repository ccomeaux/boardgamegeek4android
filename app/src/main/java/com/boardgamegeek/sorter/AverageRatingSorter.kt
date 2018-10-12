package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes

import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract.Collection

import java.text.DecimalFormat

class AverageRatingSorter(context: Context) : RatingSorter(context) {
    private val format = DecimalFormat("0.00")

    @StringRes
    override val descriptionResId = R.string.collection_sort_average_rating

    @StringRes
    public override val typeResId = R.string.collection_sort_type_average_rating

    override val sortColumn = Collection.STATS_AVERAGE

    override val displayFormat = format
}
