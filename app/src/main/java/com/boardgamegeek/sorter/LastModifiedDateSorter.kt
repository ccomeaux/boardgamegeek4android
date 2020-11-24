package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity

class LastModifiedDateSorter(context: Context) : CollectionDateSorter(context) {
    @StringRes
    public override val typeResId = R.string.collection_sort_type_last_modified

    @StringRes
    override val descriptionResId = R.string.collection_sort_last_modified

    override fun sort(items: Iterable<CollectionItemEntity>) = items.sortedByDescending { it.lastModifiedDate }

    override fun getTimestamp(item: CollectionItemEntity) = item.lastModifiedDate
}
