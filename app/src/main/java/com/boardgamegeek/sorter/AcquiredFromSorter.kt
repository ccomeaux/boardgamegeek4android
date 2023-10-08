package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItem

class AcquiredFromSorter(context: Context) : CollectionSorter(context) {
    private val nowhere = context.getString(R.string.nowhere_in_angle_brackets)

    override val ascendingSortTypeResId: Int
        @StringRes
        get() = R.string.collection_sort_type_acquired_from

    override val descendingSortTypeResId: Int
        @StringRes
        get() = R.string.collection_sort_type_acquired_from_desc

    override val descriptionResId: Int
        @StringRes
        get() = R.string.collection_sort_acquired_from

    override fun sortAscending(items: Iterable<CollectionItem>) = items.sortedBy { it.acquiredFrom }

    override fun sortDescending(items: Iterable<CollectionItem>) = items.sortedByDescending { it.acquiredFrom }

    override fun getHeaderText(item: CollectionItem) = item.acquiredFrom.ifBlank { nowhere }
}
