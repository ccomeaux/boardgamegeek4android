package com.boardgamegeek.sorter

import android.content.Context
import android.support.annotation.StringRes
import com.boardgamegeek.R

class LastPlayDateDescendingSorter(context: Context) : LastPlayDateSorter(context) {

    override val descriptionId: Int
        @StringRes
        get() = R.string.collection_sort_play_date_max

    public override val typeResource: Int
        @StringRes
        get() = R.string.collection_sort_type_play_date_max
}