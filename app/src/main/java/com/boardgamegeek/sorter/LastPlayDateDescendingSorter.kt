package com.boardgamegeek.sorter

import android.content.Context
import android.support.annotation.StringRes
import com.boardgamegeek.R

class LastPlayDateDescendingSorter(context: Context) : LastPlayDateSorter(context) {
    @StringRes
    override val descriptionResId = R.string.collection_sort_play_date_max

    @StringRes
    public override val typeResId = R.string.collection_sort_type_play_date_max
}