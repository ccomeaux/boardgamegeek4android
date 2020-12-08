package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.extensions.asYear

abstract class YearPublishedSorter(context: Context) : CollectionSorter(context) {
    @StringRes
    override val descriptionResId = R.string.collection_sort_year_published

    override fun getHeaderText(item: CollectionItemEntity) = item.gameYearPublished.asYear(context)
}
