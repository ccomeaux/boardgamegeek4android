package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes

import com.boardgamegeek.R

class PlayCountDescendingSorter(context: Context) : PlayCountSorter(context) {
    @StringRes
    override val descriptionResId = R.string.collection_sort_play_count_desc

    @StringRes
    public override val typeResId = R.string.collection_sort_type_play_count_desc

    override val isSortDescending = true
}
