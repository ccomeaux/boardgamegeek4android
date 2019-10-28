package com.boardgamegeek.filterer

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract.Games
import java.util.*

class SuggestedAgeFilterer(context: Context) : CollectionFilterer(context) {
    var min by IntervalDelegate(lowerBound, lowerBound, upperBound)
    var max by IntervalDelegate(upperBound, lowerBound, upperBound)
    var includeUndefined = false

    override val typeResourceId = R.string.collection_filter_type_suggested_age

    override fun inflate(data: String) {
        val d = data.split(DELIMITER.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
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

    override fun getSelection(): String {
        var format = when (max) {
            upperBound -> "(%1\$s>=?)"
            else -> "(%1\$s>=? AND %1\$s<=?)"
        }
        if (includeUndefined) format += " OR %1\$s=0 OR %1\$s IS NULL"
        return String.format(format, Games.MINIMUM_AGE)
    }

    override fun getSelectionArgs(): Array<String>? {
        return when (max) {
            upperBound -> arrayOf(min.toString())
            else -> arrayOf(min.toString(), max.toString())
        }
    }

    companion object {
        const val lowerBound = 1
        const val upperBound = 21
    }
}
