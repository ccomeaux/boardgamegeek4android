package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity
import java.text.NumberFormat

class PlayCountSorter(context: Context) : CollectionSorter(context) {
    override val ascendingSortTypeResId: Int
        @StringRes
        get() = R.string.collection_sort_type_play_count_asc

    override val descendingSortTypeResId: Int
        @StringRes
        get() = R.string.collection_sort_type_play_count_desc

    override val descriptionResId: Int
        @StringRes
        get() = R.string.collection_sort_play_count

    override fun sortAscending(items: Iterable<CollectionItemEntity>) = items.sortedBy { it.numberOfPlays }

    override fun sortDescending(items: Iterable<CollectionItemEntity>) = items.sortedByDescending { it.numberOfPlays }

    override fun getHeaderText(item: CollectionItemEntity): String = NumberFormat.getIntegerInstance().format(item.numberOfPlays)

    override fun getDisplayInfo(item: CollectionItemEntity) =
        context.resources.getQuantityString(R.plurals.plays, item.numberOfPlays, getHeaderText(item))
}
