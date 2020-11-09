package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes

import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity

class YearPublishedAscendingSorter(context: Context) : YearPublishedSorter(context) {
    @StringRes
    public override val typeResId = R.string.collection_sort_type_year_published_asc

    @StringRes
    public override val subDescriptionResId = R.string.oldest

    override fun sort(items: Iterable<CollectionItemEntity>) = items.sortedBy { it.gameYearPublished }
}
