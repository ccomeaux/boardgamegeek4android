package com.boardgamegeek.filterer

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.extensions.IntervalDelegate
import java.util.*

class SuggestedAgeFilterer(context: Context) : CollectionFilterer(context) {
    var min by IntervalDelegate(lowerBound, lowerBound, upperBound)
    var max by IntervalDelegate(upperBound, lowerBound, upperBound)
    var includeUndefined = false

    override val typeResourceId = R.string.collection_filter_type_suggested_age

    override fun inflate(data: String) {
        val d = data.split(DELIMITER)
        min = d.getOrNull(0)?.toIntOrNull() ?: lowerBound
        max = d.getOrNull(1)?.toIntOrNull() ?: upperBound
        includeUndefined = d.getOrNull(2) == "1"
    }

    override fun deflate() = "$min$DELIMITER$max$DELIMITER${if (includeUndefined) "1" else "0"}"

    override fun toShortDescription() = describe(R.string.and_up_suffix_abbr, R.string.unknown_abbr)

    override fun toLongDescription() = describe(R.string.and_up_suffix, R.string.unknown)

    private fun describe(@StringRes andUpResId: Int, @StringRes unknownResId: Int): String {
        val range = when {
            max == upperBound -> context.getString(andUpResId, min)
            min == max -> String.format(Locale.getDefault(), "%,d", max)
            else -> String.format(Locale.getDefault(), "%,d-%,d", min, max)
        }
        val unknown = if (includeUndefined) " (+${context.getString(unknownResId)})" else ""
        return context.getString(R.string.ages) + " " + range + unknown
    }

    override fun filter(item: CollectionItemEntity): Boolean {
        return when {
            includeUndefined -> item.minimumAge == 0
            max == upperBound -> item.minimumAge >= min
            else -> item.minimumAge in min..max
        }
    }

    companion object {
        const val lowerBound = 1
        const val upperBound = 21
    }
}
