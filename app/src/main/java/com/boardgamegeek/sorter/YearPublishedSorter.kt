package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.extensions.asYear

class YearPublishedSorter(context: Context) : CollectionSorter(context) {
    override val ascendingSortTypeResId: Int
        @StringRes
        get() = R.string.collection_sort_type_year_published_asc

    override val descendingSortTypeResId: Int
        @StringRes
        get() = R.string.collection_sort_type_year_published_desc

    override val descriptionResId: Int
        @StringRes
        get() = R.string.collection_sort_year_published

    override fun sortAscending(items: Iterable<CollectionItemEntity>) = items.sortedBy { it.gameYearPublished }

    override fun sortDescending(items: Iterable<CollectionItemEntity>) = items.sortedByDescending { it.gameYearPublished }

    override fun getHeaderText(item: CollectionItemEntity) = item.gameYearPublished.asYear(context)
}
