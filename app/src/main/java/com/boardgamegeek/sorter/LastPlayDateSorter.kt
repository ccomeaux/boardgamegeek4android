package com.boardgamegeek.sorter

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract.Plays

abstract class LastPlayDateSorter(context: Context) : CollectionDateSorter(context) {
    override val sortColumn = Plays.MAX_DATE

    override val defaultValueResId = R.string.never
}
