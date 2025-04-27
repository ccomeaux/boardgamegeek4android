package com.boardgamegeek.filterer

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.extensions.IntervalDelegate
import java.util.*

class PlayerNumberFilterer(context: Context) : CollectionFilterer(context) {
    var min by IntervalDelegate(LOWER_BOUND, LOWER_BOUND, UPPER_BOUND)
    var max by IntervalDelegate(UPPER_BOUND, LOWER_BOUND, UPPER_BOUND)
    var isExact = false

    override val typeResourceId = R.string.collection_filter_type_number_of_players

    override fun inflate(data: String) {
        val d = data.split(DELIMITER)
        min = d.getOrNull(0)?.toIntOrNull() ?: LOWER_BOUND
        max = d.getOrNull(1)?.toIntOrNull() ?: UPPER_BOUND
        isExact = d.getOrNull(2) == "1"
    }

    override fun deflate() = "$min$DELIMITER$max$DELIMITER${if (isExact) "1" else "0"}"

    override val iconResourceId: Int
        get() = R.drawable.ic_baseline_group_24

    override fun chipText(): String {
        return "${if (isExact) "${context.getString(R.string.exactly)} " else ""}${describeRange()}"
    }

    override fun description(): String {
        return "${chipText()} ${context.getString(R.string.players)}"
    }

    fun describeRange(rangeDelimiter: String = "-") = when (min) {
        max -> String.format(Locale.getDefault(), "%,d", max)
        else -> String.format(Locale.getDefault(), "%,d$rangeDelimiter%,d", min, max)
    }

    override fun filter(item: CollectionItem): Boolean {
        return when {
            item.minPlayerCount == 0 -> false
            isExact -> item.minPlayerCount == min && item.maxPlayerCount == max
            else -> item.minPlayerCount <= min && (item.maxPlayerCount >= max || item.maxPlayerCount == 0)
        }
    }

    companion object {
        const val LOWER_BOUND = 1
        const val UPPER_BOUND = 12
    }
}
