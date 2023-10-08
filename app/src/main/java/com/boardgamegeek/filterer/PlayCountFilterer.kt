package com.boardgamegeek.filterer

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.extensions.IntervalDelegate
import java.util.*

class PlayCountFilterer(context: Context) : CollectionFilterer(context) {
    var min by IntervalDelegate(lowerBound, lowerBound, upperBound)
    var max by IntervalDelegate(upperBound, lowerBound, upperBound)

    override val typeResourceId = R.string.collection_filter_type_play_count

    override fun inflate(data: String) {
        data.split(DELIMITER).run {
            min = getOrNull(0)?.toIntOrNull() ?: lowerBound
            max = getOrNull(1)?.toIntOrNull() ?: upperBound
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
        max >= upperBound -> String.format(Locale.getDefault(), "%,d+", min)
        min == max -> String.format(Locale.getDefault(), "%,d", max)
        min <= lowerBound -> String.format(Locale.getDefault(), "<%,d", max)
        else -> String.format(Locale.getDefault(), "%,d$rangeDelimiter%,d", min, max)
    }

    override fun filter(item: CollectionItem): Boolean {
        return when {
            max >= upperBound -> item.numberOfPlays >= min
            else -> item.numberOfPlays in min..max
        }
    }

    companion object {
        const val lowerBound = 0
        const val upperBound = 25
    }
}
