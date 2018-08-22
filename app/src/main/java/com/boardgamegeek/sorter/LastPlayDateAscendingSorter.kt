package com.boardgamegeek.sorter

import android.content.Context
import android.support.annotation.StringRes
import com.boardgamegeek.R

class LastPlayDateAscendingSorter(context: Context) : LastPlayDateSorter(context) {
    @StringRes
    override val descriptionResId = R.string.collection_sort_play_date_min

    @StringRes
    public override val typeResId = R.string.collection_sort_type_play_date_min

    override val isSortDescending = false
}