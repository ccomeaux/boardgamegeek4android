package com.boardgamegeek.filterer

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.extensions.andLess
import com.boardgamegeek.extensions.andMore
import com.boardgamegeek.extensions.asTime
import com.boardgamegeek.provider.BggContract.Games

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
            min == lowerBound && max == upperBound -> ""
            max == lowerBound -> max.asTime()
            min == lowerBound -> max.asTime().andLess()
            max == upperBound -> min.asTime().andMore()
            min == max -> max.asTime()
            else -> "${min.asTime()}-${max.asTime()}"
        }
        val unknown = if (includeUndefined) " (+${context.getString(unknownResId)})" else ""
        return range + unknown
    }

    override fun getSelection(): String {
        var format = when {
            min == lowerBound -> "(%1\$s<=?)"
            max == upperBound -> "(%1\$s>=?)"
            else -> "(%1\$s>=? AND %1\$s<=?)"
        }
        if (includeUndefined) format += " OR %1\$s IS NULL"
        return String.format(format, Games.PLAYING_TIME)
    }

    override fun getSelectionArgs(): Array<String>? {
        return when {
            min == lowerBound -> arrayOf(max.toString())
            max == upperBound -> arrayOf(min.toString())
            else -> arrayOf(min.toString(), max.toString())
        }
    }

    companion object {
        const val lowerBound = 0
        const val upperBound = 360 // 6 hours
    }
}
