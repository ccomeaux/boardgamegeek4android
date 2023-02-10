package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.extensions.asPastDaySpan

class LastViewedSorter(context: Context) : CollectionSorter(context) {
    override val ascendingSortTypeResId: Int
        @StringRes
        get() = R.string.collection_sort_type_last_viewed_asc

    override val descendingSortTypeResId: Int
        @StringRes
        get() = R.string.collection_sort_type_last_viewed

    override val descriptionResId: Int
        @StringRes
        get() = R.string.collection_sort_last_viewed

    override fun sortAscending(items: Iterable<CollectionItemEntity>) = items.sortedBy { it.lastViewedDate }

    override fun sortDescending(items: Iterable<CollectionItemEntity>) = items.sortedByDescending { it.lastViewedDate }

    override fun getHeaderText(item: CollectionItemEntity) = item.lastViewedDate.asPastDaySpan(context).toString()

    override fun getDisplayInfo(item: CollectionItemEntity) = ""

    override fun getTimestamp(item: CollectionItemEntity) = item.lastViewedDate
}
