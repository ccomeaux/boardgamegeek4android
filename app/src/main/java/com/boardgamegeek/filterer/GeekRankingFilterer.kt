package com.boardgamegeek.filterer

import android.content.Context
import android.support.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract.Collection
import java.util.*

class GeekRankingFilterer(context: Context) : CollectionFilterer(context) {
    var min by IntervalDelegate(lowerBound, lowerBound, upperBound)
    var max by IntervalDelegate(upperBound, lowerBound, upperBound)
    var includeUnranked = false

    override val typeResourceId = R.string.collection_filter_type_geek_ranking

    override fun inflate(data: String) {
        val d = data.split(DELIMITER.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        min = d.getOrNull(0)?.toIntOrNull() ?: lowerBound
        max = d.getOrNull(1)?.toIntOrNull() ?: upperBound
        includeUnranked = d.getOrNull(2) == "1"
    }

    override fun deflate() = "$min$DELIMITER$max$DELIMITER${if (includeUnranked) "1" else "0"}"

    override fun toShortDescription() = describe(R.string.unranked_abbr)

    override fun toLongDescription() = context.getString(R.string.ranked) + " " + describe(R.string.unranked)

    private fun describe(@StringRes unrankedResId: Int): String {
        val range: String = when {
            max == upperBound -> String.format(Locale.getDefault(), "%,d+", min)
            min == max -> String.format(Locale.getDefault(), "%,d", max)
            min == lowerBound -> String.format(Locale.getDefault(), "<%,d", max)
            else -> String.format(Locale.getDefault(), "%,d-%,d", min, max)
        }
        val unranked = if (includeUnranked) " (+${context.getString(unrankedResId)})" else ""
        return "#$range$unranked"
    }

    override fun getSelection(): String {
        var format = when {
            max == upperBound -> "%1\$s>=?"
            min == max -> "%1\$s=?"
            min == lowerBound -> "%1\$s<=?"
            else -> "(%1\$s>=? AND %1\$s<=?)"
        }
        if (includeUnranked) format += " OR %1\$s=0 OR %1\$s IS NULL"
        return String.format(format, Collection.GAME_RANK)
    }

    override fun getSelectionArgs(): Array<String>? {
        return when {
            max == upperBound -> arrayOf(min.toString())
            min == max -> arrayOf(min.toString())
            min == lowerBound -> arrayOf(max.toString())
            else -> arrayOf(min.toString(), max.toString())
        }
    }

    companion object {
        const val lowerBound = 1
        const val upperBound = 2000
    }
}
