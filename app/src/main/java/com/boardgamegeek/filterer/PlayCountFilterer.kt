package com.boardgamegeek.filterer

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.extensions.IntervalDelegate
import java.util.*

class PlayCountFilterer(context: Context) : CollectionFilterer(context) {
    var min by IntervalDelegate(LOWER_BOUND, LOWER_BOUND, UPPER_BOUND)
    var max by IntervalDelegate(UPPER_BOUND, LOWER_BOUND, UPPER_BOUND)

    override val typeResourceId = R.string.collection_filter_type_play_count

    override fun inflate(data: String) {
        data.split(DELIMITER).run {
            min = getOrNull(0)?.toIntOrNull() ?: LOWER_BOUND
            max = getOrNull(1)?.toIntOrNull() ?: UPPER_BOUND
        }
    }

    override fun deflate() = "$min$DELIMITER$max"

    override val iconResourceId: Int
        get() = R.drawable.ic_baseline_event_available_24

    override fun chipText(): String {
        return describeRange()
    }

    override fun description(): String {
        return "${describeRange(" - ")} ${context.getString(R.string.plays)}"
    }

    fun describeRange(rangeDelimiter: String = "-") = when {
        max >= UPPER_BOUND -> String.format(Locale.getDefault(), "%,d+", min)
        min == max -> String.format(Locale.getDefault(), "%,d", max)
        min <= LOWER_BOUND -> String.format(Locale.getDefault(), "<%,d", max)
        else -> String.format(Locale.getDefault(), "%,d$rangeDelimiter%,d", min, max)
    }

    override fun filter(item: CollectionItem): Boolean {
        return when {
            max >= UPPER_BOUND -> item.numberOfPlays >= min
            else -> item.numberOfPlays in min..max
        }
    }

    companion object {
        const val LOWER_BOUND = 0
        const val UPPER_BOUND = 25
    }
}
