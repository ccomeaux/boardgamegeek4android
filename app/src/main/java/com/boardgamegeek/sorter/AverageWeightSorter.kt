package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItem
import java.text.DecimalFormat

class AverageWeightSorter(context: Context) : CollectionSorter(context) {
    private val defaultValue = context.getString(R.string.text_unknown)

    override val ascendingSortTypeResId: Int
        @StringRes
        get() = R.string.collection_sort_type_average_weight_asc

    override val descendingSortTypeResId: Int
        @StringRes
        get() = R.string.collection_sort_type_average_weight_desc

    override val descriptionResId: Int
        @StringRes
        get() = R.string.collection_sort_average_weight

    override fun sortAscending(items: Iterable<CollectionItem>) = items.sortedBy { it.averageWeight }

    override fun sortDescending(items: Iterable<CollectionItem>) = items.sortedByDescending { it.averageWeight }

    override fun getHeaderText(item: CollectionItem): String {
        val averageWeight = item.averageWeight
        return if (averageWeight == 0.0) defaultValue else DecimalFormat("#.0").format(averageWeight)
    }

    override fun getDisplayInfo(item: CollectionItem): String {
        val averageWeight = item.averageWeight
        val info = if (averageWeight == 0.0) defaultValue else DecimalFormat("0.000").format(averageWeight)
        return "${context.getString(R.string.weight)} $info"
    }
}
