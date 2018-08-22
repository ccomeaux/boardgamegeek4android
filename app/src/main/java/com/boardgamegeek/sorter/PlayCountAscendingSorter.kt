package com.boardgamegeek.sorter

import android.content.Context
import android.support.annotation.StringRes

import com.boardgamegeek.R

class PlayCountAscendingSorter(context: Context) : PlayCountSorter(context) {
    @StringRes
    override val descriptionResId = R.string.collection_sort_play_count_asc

    @StringRes
    public override val typeResId = R.string.collection_sort_type_play_count_asc
}
