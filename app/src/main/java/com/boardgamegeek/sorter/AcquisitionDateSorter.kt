package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.model.CollectionItem

class AcquisitionDateSorter(context: Context) : CollectionDateSorter(context) {
    override val ascendingSortTypeResId: Int
        @StringRes
        get() = R.string.collection_sort_type_acquisition_date_asc

    override val descendingSortTypeResId: Int
        @StringRes
        get() = R.string.collection_sort_type_acquisition_date

    override val descriptionResId: Int
        @StringRes
        get() = R.string.collection_sort_acquisition_date

    override fun sortAscending(items: Iterable<CollectionItem>) = items.sortedBy { it.acquisitionDate }

    override fun sortDescending(items: Iterable<CollectionItem>) = items.sortedByDescending { it.acquisitionDate }

    override fun getTimestamp(item: CollectionItem) = item.acquisitionDate
}
