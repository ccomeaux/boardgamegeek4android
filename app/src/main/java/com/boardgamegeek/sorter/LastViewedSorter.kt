package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItem
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

    override fun sortAscending(items: Iterable<CollectionItem>) = items.sortedBy { it.lastViewedDate }

    override fun sortDescending(items: Iterable<CollectionItem>) = items.sortedByDescending { it.lastViewedDate }

    override fun getHeaderText(item: CollectionItem) = item.lastViewedDate.asPastDaySpan(context).toString()

    override fun getDisplayInfo(item: CollectionItem) = ""

    override fun getTimestamp(item: CollectionItem) = item.lastViewedDate
}
