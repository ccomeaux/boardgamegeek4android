package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.extensions.asBoundedRating
import com.boardgamegeek.extensions.firstChar

class CollectionNameSorter(context: Context) : CollectionSorter(context) {
    public override val ascendingSortTypeResId: Int
        @StringRes
        get() = R.string.collection_sort_type_collection_name

    public override val descendingSortTypeResId: Int
        @StringRes
        get() = R.string.collection_sort_type_collection_name_desc

    override val descriptionResId: Int
        @StringRes
        get() = R.string.collection_sort_collection_name

    override fun sortAscending(items: Iterable<CollectionItemEntity>): List<CollectionItemEntity> {
        return items.sortedBy { it.sortName } // Needs to be case insensitive?
    }

    override fun sortDescending(items: Iterable<CollectionItemEntity>) = items.sortedByDescending { it.sortName }

    override fun getHeaderText(item: CollectionItemEntity) = item.sortName.firstChar()

    override fun getRating(item: CollectionItemEntity): Double = item.averageRating

    override fun getRatingText(item: CollectionItemEntity) = getRating(item).asBoundedRating(context, R.string.unrated_abbr)
}
