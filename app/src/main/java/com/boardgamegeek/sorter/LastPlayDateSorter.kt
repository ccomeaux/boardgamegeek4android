package com.boardgamegeek.sorter

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.provider.BggContract.Plays

abstract class LastPlayDateSorter(context: Context) : CollectionDateSorter(context) {
    override val sortColumn = Plays.MAX_DATE

    override val defaultValueResId = R.string.never

    override fun getTimestamp(item: CollectionItemEntity) = item.lastPlayDate
}
