package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity
import java.text.DecimalFormat

class GeekRatingSorter(context: Context) : RatingSorter(context) {
    override val ascendingSortTypeResId: Int
        @StringRes
        get() = R.string.collection_sort_type_geek_rating_asc

    override val descendingSortTypeResId: Int
        @StringRes
        get() = R.string.collection_sort_type_geek_rating

    override val descriptionResId: Int
        @StringRes
        get() = R.string.collection_sort_geek_rating

    override fun sortAscending(items: Iterable<CollectionItemEntity>) = items.sortedBy { it.geekRating }

    override fun sortDescending(items: Iterable<CollectionItemEntity>) = items.sortedByDescending { it.geekRating }

    override val displayFormat = DecimalFormat("0.000")

    override fun getRating(item: CollectionItemEntity) = item.geekRating
}
