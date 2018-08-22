package com.boardgamegeek.sorter

import android.content.Context
import android.support.annotation.StringRes

import com.boardgamegeek.R

class PlayTimeAscendingSorter(context: Context) : PlayTimeSorter(context) {
    @StringRes
    public override val subDescriptionResId = R.string.shortest

    @StringRes
    public override val typeResId = R.string.collection_sort_type_play_time_asc
}
