package com.boardgamegeek.sorter

import android.content.Context
import android.support.annotation.StringRes

import com.boardgamegeek.R

class TotalTimePlayedAscendingSorter(context: Context) : TotalTimePlayedSorter(context) {

    public override val typeResource: Int
        @StringRes
        get() = R.string.collection_sort_type_total_played_time_asc

    public override val subDescriptionId: Int
        @StringRes
        get() = R.string.shortest
}
