package com.boardgamegeek.sorter

import android.content.Context
import android.support.annotation.StringRes
import com.boardgamegeek.R

class LastPlayDateAscendingSorter(context: Context) : LastPlayDateSorter(context) {

    override val descriptionId: Int
        @StringRes
        get() = R.string.collection_sort_play_date_min

    override val isSortDescending: Boolean
        get() = false

    public override val typeResource: Int
        @StringRes
        get() = R.string.collection_sort_type_play_date_min
}