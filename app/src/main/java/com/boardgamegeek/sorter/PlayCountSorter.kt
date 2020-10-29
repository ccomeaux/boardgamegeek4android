package com.boardgamegeek.sorter

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.provider.BggContract.Collection
import java.text.NumberFormat

abstract class PlayCountSorter(context: Context) : CollectionSorter(context) {
    override val sortColumn = Collection.NUM_PLAYS

    override fun getHeaderText(item: CollectionItemEntity): String =
            NumberFormat.getIntegerInstance().format(item.numberOfPlays)

    override fun getDisplayInfo(item: CollectionItemEntity) = context.resources.getQuantityString(R.plurals.plays, item.numberOfPlays, getHeaderText(item))
}
