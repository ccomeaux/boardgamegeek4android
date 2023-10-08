package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.extensions.asBoundedRating
import com.boardgamegeek.extensions.firstChar
import java.text.DecimalFormat

class CollectionNameSorter(context: Context) : CollectionSorter(context) {
    private val displayFormat = DecimalFormat("0.00")

    public override val ascendingSortTypeResId: Int
        @StringRes
        get() = R.string.collection_sort_type_collection_name

    public override val descendingSortTypeResId: Int
        @StringRes
        get() = R.string.collection_sort_type_collection_name_desc

    override val descriptionResId: Int
        @StringRes
        get() = R.string.collection_sort_collection_name

    override fun sortAscending(items: Iterable<CollectionItem>): List<CollectionItem> {
        return items.sortedBy { it.sortName } // Needs to be case insensitive?
    }

    override fun sortDescending(items: Iterable<CollectionItem>) = items.sortedByDescending { it.sortName }

    override fun getHeaderText(item: CollectionItem) = item.sortName.firstChar()

    override fun getRating(item: CollectionItem): Double = item.averageRating

    override fun getRatingText(item: CollectionItem) = getRating(item).asBoundedRating(context, displayFormat, R.string.unrated_abbr)
}
