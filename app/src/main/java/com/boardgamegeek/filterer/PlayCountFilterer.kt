package com.boardgamegeek.filterer

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract.Collection
import java.util.*

class PlayCountFilterer(context: Context) : CollectionFilterer(context) {
    var min by IntervalDelegate(lowerBound, lowerBound, upperBound)
    var max by IntervalDelegate(upperBound, lowerBound, upperBound)

    override val typeResourceId = R.string.collection_filter_type_play_count

    override fun inflate(data: String) {
        val d = data.split(DELIMITER.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        min = d.getOrNull(0)?.toIntOrNull() ?: lowerBound
        max = d.getOrNull(1)?.toIntOrNull() ?: upperBound
    }

    override fun deflate() = "$min$DELIMITER$max"

    override fun toShortDescription(): String {
        val text = when {
            max >= upperBound -> String.format(Locale.getDefault(), "%,d+", min)
            min == max -> String.format(Locale.getDefault(), "%,d", max)
            min <= lowerBound -> String.format(Locale.getDefault(), "<%,d", max)
            else -> String.format(Locale.getDefault(), "%,d-%,d", min, max)
        }
        return text + " " + context.getString(R.string.plays)
    }

    override fun getSelection(): String {
        val format = when {
            max >= upperBound -> "%1\$s>=?"
            else -> "(%1\$s>=? AND %1\$s<=?)"
        }
        return String.format(format, Collection.NUM_PLAYS)
    }

    override fun getSelectionArgs(): Array<String>? {
        return when {
            max >= upperBound -> arrayOf(min.toString())
            else -> arrayOf(min.toString(), max.toString())
        }
    }

    companion object {
        const val lowerBound = 0
        const val upperBound = 25
    }
}
