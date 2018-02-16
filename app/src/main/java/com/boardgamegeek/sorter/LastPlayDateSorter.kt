package com.boardgamegeek.sorter

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract.Plays

abstract class LastPlayDateSorter(context: Context) : CollectionDateSorter(context) {

    override val sortColumn: String
        get() = Plays.MAX_DATE

    override val defaultValue: String
        get() = context.getString(R.string.never)

}
