package com.boardgamegeek.filterer

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.clamp
import com.boardgamegeek.provider.BggContract.Collection

class PlayCountFilterer(context: Context) : CollectionFilterer(context) {
    var min = MIN_RANGE
    var max = MAX_RANGE

    override val typeResourceId = R.string.collection_filter_type_play_count

    override fun inflate(data: String) {
        val d = data.split(DELIMITER.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        min = d.getOrNull(0)?.toIntOrNull()?.clamp(MIN_RANGE, MAX_RANGE) ?: MIN_RANGE
        max = d.getOrNull(1)?.toIntOrNull()?.clamp(MIN_RANGE, MAX_RANGE) ?: MAX_RANGE
    }

    override fun deflate() = "$min$DELIMITER$max"

    override fun toShortDescription(): String {
        val text = when {
            max >= MAX_RANGE -> min.toString() + "+"
            min == max -> max.toString()
            else -> min.toString() + "-" + max
        }
        return text + " " + context.getString(R.string.plays)
    }

    override fun getSelection(): String {
        val format = when {
            max >= MAX_RANGE -> "%1\$s>=?"
            else -> "(%1\$s>=? AND %1\$s<=?)"
        }
        return String.format(format, Collection.NUM_PLAYS)
    }

    override fun getSelectionArgs(): Array<String>? {
        return when {
            max >= MAX_RANGE -> arrayOf(min.toString())
            else -> arrayOf(min.toString(), max.toString())
        }
    }

    companion object {
        const val MIN_RANGE = 0
        const val MAX_RANGE = 25
    }
}
