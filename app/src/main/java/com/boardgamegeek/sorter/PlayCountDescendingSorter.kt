package com.boardgamegeek.sorter

import android.content.Context
import android.support.annotation.StringRes

import com.boardgamegeek.R

class PlayCountDescendingSorter(context: Context) : PlayCountSorter(context) {

    override val isSortDescending: Boolean
        get() = true

    override val descriptionId: Int
        @StringRes
        get() = R.string.collection_sort_play_count_desc

    public override val typeResource: Int
        @StringRes
        get() = R.string.collection_sort_type_play_count_desc
}
