package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItem
import java.text.DecimalFormat

class AverageRatingSorter(context: Context) : RatingSorter(context) {
    override val ascendingSortTypeResId: Int
        @StringRes
        get() = R.string.collection_sort_type_average_rating_asc

    override val descendingSortTypeResId: Int
        @StringRes
        get() = R.string.collection_sort_type_average_rating

    override val descriptionResId: Int
        @StringRes
        get() = R.string.collection_sort_average_rating

    override fun sortAscending(items: Iterable<CollectionItem>) = items.sortedBy { it.averageRating }

    override fun sortDescending(items: Iterable<CollectionItem>) = items.sortedByDescending { it.averageRating }

    override val displayFormat = DecimalFormat("0.00")

    override fun getRating(item: CollectionItem) = item.averageRating
}
