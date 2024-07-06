package com.boardgamegeek.sorter

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.extensions.asBoundedRating
import java.text.DecimalFormat

abstract class RatingSorter(context: Context) : CollectionSorter(context) {
    private val defaultValue: String = context.getString(R.string.text_unknown)

    protected abstract val displayFormat: DecimalFormat

    override fun getHeaderText(item: CollectionItem): String {
        val rating = getRating(item)
        return if (rating == 0.0) defaultValue else rating.asBoundedRating(context, DecimalFormat("#0.0"), R.string.unrated_abbr)
    }

    override fun getRatingText(item: CollectionItem) = getRating(item).asBoundedRating(context, displayFormat, R.string.unrated_abbr)
}
