package com.boardgamegeek.sorter

import android.content.Context
import android.support.annotation.StringRes

import com.boardgamegeek.R

class PlayTimeAscendingSorter(context: Context) : PlayTimeSorter(context) {

    public override val typeResource: Int
        @StringRes
        get() = R.string.collection_sort_type_play_time_asc

    public override val subDescriptionId: Int
        @StringRes
        get() = R.string.shortest
}
