package com.boardgamegeek.sorter

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity

abstract class LastPlayDateSorter(context: Context) : CollectionDateSorter(context) {
    override val defaultValueResId = R.string.never

    override fun getTimestamp(item: CollectionItemEntity) = item.lastPlayDate
}
