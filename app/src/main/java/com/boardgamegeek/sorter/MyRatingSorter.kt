package com.boardgamegeek.sorter

import android.content.Context
import android.support.annotation.StringRes

import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract.Collection

import java.text.DecimalFormat

class MyRatingSorter(context: Context) : RatingSorter(context) {
    private val format = DecimalFormat("0.0")

    override val descriptionId: Int
        @StringRes
        get() = R.string.collection_sort_my_rating

    public override val typeResource: Int
        @StringRes
        get() = R.string.collection_sort_type_my_rating

    override val sortColumn: String
        get() = Collection.RATING

    override val displayFormat: DecimalFormat
        get() = format
}
