package com.boardgamegeek.sorter

import android.content.Context
import android.support.annotation.StringRes

import com.boardgamegeek.R

class YearPublishedAscendingSorter(context: Context) : YearPublishedSorter(context) {

    public override val typeResource: Int
        @StringRes
        get() = R.string.collection_sort_type_year_published_asc

    public override val subDescriptionId: Int
        @StringRes
        get() = R.string.oldest
}
