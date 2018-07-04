package com.boardgamegeek.filterer

import android.content.Context
import android.support.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract.Games
import java.util.*

class PlayTimeFilterer(context: Context) : CollectionFilterer(context) {
    var min by IntervalDelegate(lowerBound, lowerBound, upperBound)
    var max by IntervalDelegate(upperBound, lowerBound, upperBound)
    var includeUndefined = false

    override val typeResourceId = R.string.collection_filter_type_play_time

    override fun inflate(data: String) {
        val d = data.split(DELIMITER.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        min = d.getOrNull(0)?.toIntOrNull() ?: lowerBound
        max = d.getOrNull(1)?.toIntOrNull() ?: upperBound
        includeUndefined = d.getOrNull(2) == "1"
    }

    override fun deflate() = "$min$DELIMITER$max$DELIMITER${if (includeUndefined) "1" else "0"}"

    override fun toShortDescription() = describe(R.string.unknown_abbr)

    override fun toLongDescription() = describe(R.string.unknown)

    private fun describe(@StringRes unknownResId: Int): String {
        val range = when {
            max == upperBound -> context.getString(R.string.and_up_suffix_abbr, min)
            min == max -> String.format(Locale.getDefault(), "%,d", max)
            else -> String.format(Locale.getDefault(), "%,d-%,d", min, max)
        }
        val unknown = if (includeUndefined) " (+${context.getString(unknownResId)})" else ""
        return range + " " + context.getString(R.string.minutes_abbr) + unknown
    }

    override fun getSelection(): String {
        var format = when (max) {
            upperBound -> "(%1\$s>=?)"
            else -> "(%1\$s>=? AND %1\$s<=?)"
        }
        if (includeUndefined) format += " OR %1\$s IS NULL"
        return String.format(format, Games.PLAYING_TIME)
    }

    override fun getSelectionArgs(): Array<String>? {
        return when (max) {
            upperBound -> arrayOf(min.toString())
            else -> arrayOf(min.toString(), max.toString())
        }
    }

    companion object {
        const val lowerBound = 0
        const val upperBound = 300
    }
}
