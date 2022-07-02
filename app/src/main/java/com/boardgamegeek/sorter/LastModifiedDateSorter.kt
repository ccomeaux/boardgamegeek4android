package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity

class LastModifiedDateSorter(context: Context) : CollectionDateSorter(context) {
    override val ascendingSortTypeResId: Int
        @StringRes
        get() = R.string.collection_sort_type_last_modified_asc

    override val descendingSortTypeResId: Int
        @StringRes
        get() = R.string.collection_sort_type_last_modified

    override val descriptionResId: Int
        @StringRes
        get() = R.string.collection_sort_last_modified

    override fun sortAscending(items: Iterable<CollectionItemEntity>) = items.sortedBy { it.lastModifiedDate }

    override fun sortDescending(items: Iterable<CollectionItemEntity>) = items.sortedByDescending { it.lastModifiedDate }

    override fun getTimestamp(item: CollectionItemEntity) = item.lastModifiedDate
}
