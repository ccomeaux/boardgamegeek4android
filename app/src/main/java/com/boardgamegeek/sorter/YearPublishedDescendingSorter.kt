package com.boardgamegeek.sorter

import android.content.Context
import android.support.annotation.StringRes

import com.boardgamegeek.R

class YearPublishedDescendingSorter(context: Context) : YearPublishedSorter(context) {

    public override val typeResource: Int
        @StringRes
        get() = R.string.collection_sort_type_year_published_desc

    override val isSortDescending: Boolean
        get() = true

    public override val subDescriptionId: Int
        @StringRes
        get() = R.string.newest
}
