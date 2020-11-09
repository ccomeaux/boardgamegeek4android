package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity
import java.text.DecimalFormat

abstract class AverageWeightSorter(context: Context) : CollectionSorter(context) {
    private val defaultValue = context.getString(R.string.text_unknown)

    @StringRes
    override val descriptionResId = R.string.collection_sort_average_weight

    override fun getHeaderText(item: CollectionItemEntity): String {
        val averageWeight = item.averageWeight
        return if (averageWeight == 0.0) defaultValue else DecimalFormat("#.0").format(averageWeight)
    }

    override fun getDisplayInfo(item: CollectionItemEntity): String {
        val averageWeight = item.averageWeight
        val info = if (averageWeight == 0.0) defaultValue else DecimalFormat("0.000").format(averageWeight)
        return "${context.getString(R.string.weight)} $info"
    }
}
