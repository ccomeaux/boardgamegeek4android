package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes

import com.boardgamegeek.R

class YearPublishedDescendingSorter(context: Context) : YearPublishedSorter(context) {
    @StringRes
    public override val typeResId = R.string.collection_sort_type_year_published_desc

    @StringRes
    public override val subDescriptionResId = R.string.newest

    override val isSortDescending = true
}
