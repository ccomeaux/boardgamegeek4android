package com.boardgamegeek.filterer

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.extensions.IntervalDelegate
import com.boardgamegeek.extensions.andLess
import com.boardgamegeek.extensions.andMore
import com.boardgamegeek.extensions.asTime

class PlayTimeFilterer(context: Context) : CollectionFilterer(context) {
    var min by IntervalDelegate(lowerBound, lowerBound, upperBound)
    var max by IntervalDelegate(upperBound, lowerBound, upperBound)
    var includeUndefined = false

    override val typeResourceId = R.string.collection_filter_type_play_time

    override fun inflate(data: String) {
        val d = data.split(DELIMITER)
        min = d.getOrNull(0)?.toIntOrNull() ?: lowerBound
        max = d.getOrNull(1)?.toIntOrNull() ?: upperBound
        includeUndefined = d.getOrNull(2) == "1"
    }

    override fun deflate() = "$min$DELIMITER$max$DELIMITER${if (includeUndefined) "1" else "0"}"

    override fun toShortDescription() = describe(R.string.unknown_abbr)

    override fun toLongDescription() = describe(R.string.unknown)

    private fun describe(@StringRes unknownResId: Int): String {
        val range = describeRange()
        val unknown = if (includeUndefined) " (+${context.getString(unknownResId)})" else ""
        return range + unknown
    }

    fun describeRange(delimiter: String = "-") = when {
        min == lowerBound && max == upperBound -> ""
        max == lowerBound -> max.asTime()
        min == lowerBound -> max.asTime().andLess()
        max == upperBound -> min.asTime().andMore()
        min == max -> max.asTime()
        else -> "${min.asTime()}$delimiter${max.asTime()}"
    }

    override fun filter(item: CollectionItemEntity): Boolean {
        return when {
            item.playingTime == 0 -> includeUndefined
            min == lowerBound -> item.playingTime <= max
            max == upperBound -> item.playingTime >= min
            else -> item.playingTime in min..max
        }
    }

    companion object {
        const val lowerBound = 0
        const val upperBound = 360 // 6 hours
    }
}
