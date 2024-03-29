package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity

class SuggestedAgeSorter(context: Context) : CollectionSorter(context) {
    private val defaultValue = context.getString(R.string.text_unknown)

    override val ascendingSortTypeResId: Int
        @StringRes
        get() = R.string.collection_sort_type_suggested_age_asc

    override val descendingSortTypeResId: Int
        @StringRes
        get() = R.string.collection_sort_type_suggested_age_desc

    override val descriptionResId: Int
        @StringRes
        get() = R.string.collection_sort_suggested_age

    override fun sortAscending(items: Iterable<CollectionItemEntity>) = items.sortedBy { it.minimumAge }

    override fun sortDescending(items: Iterable<CollectionItemEntity>) = items.sortedByDescending { it.minimumAge }

    override fun getHeaderText(item: CollectionItemEntity) = if (item.minimumAge == 0) defaultValue else item.minimumAge.toString()

    override fun getDisplayInfo(item: CollectionItemEntity): String {
        val info = getHeaderText(item)
        return when {
            defaultValue != info -> "${context.getString(R.string.ages)} $info+"
            else -> "${context.getString(R.string.ages)} $info"
        }
    }
}
