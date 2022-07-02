package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity

class LastPlayDateSorter(context: Context) : CollectionDateSorter(context) {
    override val ascendingSortTypeResId: Int
        @StringRes
        get() = R.string.collection_sort_type_play_date_min

    override val descendingSortTypeResId: Int
        @StringRes
        get() = R.string.collection_sort_type_play_date_max

    override val descriptionResId: Int
        @StringRes
        get() = R.string.collection_sort_play_date

    override val defaultValueResId = R.string.never

    override fun getTimestamp(item: CollectionItemEntity) = item.lastPlayDate

    override fun sortAscending(items: Iterable<CollectionItemEntity>) = items.sortedBy { it.lastPlayDate }

    override fun sortDescending(items: Iterable<CollectionItemEntity>) = items.sortedByDescending { it.lastPlayDate }
}
