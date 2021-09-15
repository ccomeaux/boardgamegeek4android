package com.boardgamegeek.sorter

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity
import java.text.NumberFormat

abstract class PlayCountSorter(context: Context) : CollectionSorter(context) {
    override fun getHeaderText(item: CollectionItemEntity): String =
            NumberFormat.getIntegerInstance().format(item.numberOfPlays)

    override fun getDisplayInfo(item: CollectionItemEntity) = context.resources.getQuantityString(R.plurals.plays, item.numberOfPlays, getHeaderText(item))
}
