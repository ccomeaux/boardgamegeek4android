package com.boardgamegeek.sorter

import android.content.Context
import android.support.annotation.StringRes

import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract.Plays

class LastPlayDateSorter(context: Context) : CollectionDateSorter(context) {

    override val descriptionId: Int
        @StringRes
        get() = R.string.collection_sort_play_date_max

    override val sortColumn: String
        get() = Plays.MAX_DATE

    public override val typeResource: Int
        @StringRes
        get() = R.string.collection_sort_type_play_date_max
}
