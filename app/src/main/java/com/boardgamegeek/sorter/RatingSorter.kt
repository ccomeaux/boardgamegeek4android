package com.boardgamegeek.sorter

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.extensions.asRating
import com.boardgamegeek.extensions.asScore
import java.text.DecimalFormat

abstract class RatingSorter(context: Context) : CollectionSorter(context) {
    private val defaultValue: String = context.getString(R.string.text_unknown)

    override val isSortDescending = true

    protected abstract val displayFormat: DecimalFormat

    override fun getHeaderText(item: CollectionItemEntity): String {
        val rating = getRating(item)
        return if (rating == 0.0) defaultValue else rating.asScore(context, R.string.unrated_abbr, DecimalFormat("#0.#"))
    }

    override fun getRatingText(item: CollectionItemEntity) = getRating(item).asRating(context, R.string.unrated_abbr)
}
