package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity

class AcquiredFromSorter(context: Context) : CollectionSorter(context) {
    private val nowhere = context.getString(R.string.nowhere_in_angle_brackets)

    @StringRes
    public override val typeResId = R.string.collection_sort_type_acquired_from

    @StringRes
    override val descriptionResId = R.string.collection_sort_acquired_from

    override fun sort(items: Iterable<CollectionItemEntity>): List<CollectionItemEntity> {
        return items.sortedBy { it.acquiredFrom }
    }

    override fun getHeaderText(item: CollectionItemEntity) = item.acquiredFrom.ifBlank { nowhere }
}
