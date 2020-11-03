package com.boardgamegeek.filterer

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity
import java.util.*

class PlayerNumberFilterer(context: Context) : CollectionFilterer(context) {
    var min by IntervalDelegate(lowerBound, lowerBound, upperBound)
    var max by IntervalDelegate(upperBound, lowerBound, upperBound)
    var isExact = false

    override val typeResourceId = R.string.collection_filter_type_number_of_players

    override fun inflate(data: String) {
        val d = data.split(DELIMITER)
        min = d.getOrNull(0)?.toIntOrNull() ?: lowerBound
        max = d.getOrNull(1)?.toIntOrNull() ?: upperBound
        isExact = d.getOrNull(2) == "1"
    }

    override fun deflate() = "$min$DELIMITER$max$DELIMITER${if (isExact) "1" else "0"}"

    override fun toShortDescription(): String {
        val prefix = if (isExact) context.getString(R.string.exactly) + " " else ""
        val range = when (min) {
            max -> String.format(Locale.getDefault(), "%,d", max)
            else -> String.format(Locale.getDefault(), "%,d-%,d", min, max)
        }
        return prefix + range + " " + context.getString(R.string.players)
    }

    override fun filter(item: CollectionItemEntity): Boolean {
        return when {
            isExact -> item.minPlayerCount == min && item.maxPlayerCount == max
            else -> item.minPlayerCount <= min && (item.maxPlayerCount >= max || item.maxPlayerCount == 0)
        }
    }

    companion object {
        const val lowerBound = 1
        const val upperBound = 12
    }
}
