package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes

import com.boardgamegeek.R

class PlayTimeDescendingSorter(context: Context) : PlayTimeSorter(context) {
    @StringRes
    public override val subDescriptionResId = R.string.longest

    public override val typeResId = R.string.collection_sort_type_play_time_desc

    override val isSortDescending = true
}
