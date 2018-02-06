package com.boardgamegeek.sorter

import android.content.Context
import android.support.annotation.StringRes

import com.boardgamegeek.R

class PlayTimeDescendingSorter(context: Context) : PlayTimeSorter(context) {

    public override val typeResource: Int
        @StringRes
        get() = R.string.collection_sort_type_play_time_desc

    override val isSortDescending: Boolean
        get() = true

    public override val subDescriptionId: Int
        @StringRes
        get() = R.string.longest
}
