package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItem
import com.boardgamegeek.extensions.asPersonalRating
import java.text.DecimalFormat

class MyRatingSorter(context: Context) : RatingSorter(context) {
    override val ascendingSortTypeResId: Int
        @StringRes
        get() = R.string.collection_sort_type_my_rating_asc

    override val descendingSortTypeResId: Int
        @StringRes
        get() = R.string.collection_sort_type_my_rating

    override val descriptionResId: Int
        @StringRes
        get() = R.string.collection_sort_my_rating

    override fun sortAscending(items: Iterable<CollectionItem>) = items.sortedBy { it.rating }

    override fun sortDescending(items: Iterable<CollectionItem>) = items.sortedByDescending { it.rating }

    override val displayFormat = DecimalFormat("0.0")

    override fun getRating(item: CollectionItem) = item.rating

    override fun getRatingText(item: CollectionItem) = getRating(item).asPersonalRating(context)
}
