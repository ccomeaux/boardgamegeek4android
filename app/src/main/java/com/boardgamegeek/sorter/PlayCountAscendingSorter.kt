package com.boardgamegeek.sorter

import android.content.Context
import android.support.annotation.StringRes

import com.boardgamegeek.R

class PlayCountAscendingSorter(context: Context) : PlayCountSorter(context) {

    override val descriptionId: Int
        @StringRes
        get() = R.string.collection_sort_play_count_asc

    public override val typeResource: Int
        @StringRes
        get() = R.string.collection_sort_type_play_count_asc
}
