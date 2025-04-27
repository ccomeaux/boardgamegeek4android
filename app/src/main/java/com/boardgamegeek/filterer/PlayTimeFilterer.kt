package com.boardgamegeek.filterer

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.extensions.IntervalDelegate
import com.boardgamegeek.extensions.andLess
import com.boardgamegeek.extensions.andMore
import com.boardgamegeek.extensions.asTime

class PlayTimeFilterer(context: Context) : CollectionFilterer(context) {
    var min by IntervalDelegate(LOWER_BOUND, LOWER_BOUND, UPPER_BOUND)
    var max by IntervalDelegate(UPPER_BOUND, LOWER_BOUND, UPPER_BOUND)
    var includeUndefined = false

    override val typeResourceId = R.string.collection_filter_type_play_time

    override fun inflate(data: String) {
        val d = data.split(DELIMITER)
        min = d.getOrNull(0)?.toIntOrNull() ?: LOWER_BOUND
        max = d.getOrNull(1)?.toIntOrNull() ?: UPPER_BOUND
        includeUndefined = d.getOrNull(2) == "1"
    }

    override fun deflate() = "$min$DELIMITER$max$DELIMITER${if (includeUndefined) "1" else "0"}"

    override val iconResourceId: Int
        get() = R.drawable.ic_baseline_schedule_24

    override fun chipText() = describe(R.string.unknown_abbr)

    override fun description() = "${context.getString(R.string.title_play_time)} ${describe(R.string.unknown)}"

    private fun describe(@StringRes unknownResId: Int): String {
        val range = describeRange()
        val unknown = if (includeUndefined) " (+${context.getString(unknownResId)})" else ""
        return range + unknown
    }

    fun describeRange(delimiter: String = "-") = when {
        min == LOWER_BOUND && max == UPPER_BOUND -> ""
        max == LOWER_BOUND -> max.asTime()
        min == LOWER_BOUND -> max.asTime().andLess()
        max == UPPER_BOUND -> min.asTime().andMore()
        min == max -> max.asTime()
        else -> "${min.asTime()}$delimiter${max.asTime()}"
    }

    override fun filter(item: CollectionItem): Boolean {
        return when {
            item.playingTime == 0 -> includeUndefined
            min == LOWER_BOUND -> item.playingTime <= max
            max == UPPER_BOUND -> item.playingTime >= min
            else -> item.playingTime in min..max
        }
    }

    companion object {
        const val LOWER_BOUND = 0
        const val UPPER_BOUND = 360 // 6 hours
    }
}
