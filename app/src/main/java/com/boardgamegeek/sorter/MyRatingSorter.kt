package com.boardgamegeek.sorter

import android.content.Context
import android.support.annotation.StringRes

import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract.Collection

import java.text.DecimalFormat

class MyRatingSorter(context: Context) : RatingSorter(context) {
    private val format = DecimalFormat("0.0")

    @StringRes
    override val descriptionResId = R.string.collection_sort_my_rating

    @StringRes
    public override val typeResId = R.string.collection_sort_type_my_rating

    override val sortColumn = Collection.RATING

    override val displayFormat = format
}
