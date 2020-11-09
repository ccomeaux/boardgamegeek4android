package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.extensions.asPastDaySpan

class LastViewedSorter(context: Context) : CollectionSorter(context) {
    @StringRes
    override val descriptionResId = R.string.collection_sort_last_viewed

    @StringRes
    public override val typeResId = R.string.collection_sort_type_last_viewed

    override fun sort(items: Iterable<CollectionItemEntity>) = items.sortedByDescending { it.lastViewedDate }

    override fun getHeaderText(item: CollectionItemEntity): String {
        return item.lastViewedDate.asPastDaySpan(context).toString()
    }

    override fun getDisplayInfo(item: CollectionItemEntity) = ""

    override fun getTimestamp(item: CollectionItemEntity) = item.lastViewedDate
}
