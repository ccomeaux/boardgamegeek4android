package com.boardgamegeek.filterer

import android.content.Context
import android.support.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.clamp
import com.boardgamegeek.provider.BggContract
import java.util.*

class GeekRankingFilterer(context: Context) : CollectionFilterer(context) {
    var min = MIN_RANGE
    var max = MAX_RANGE
    var includeUnranked = false

    override val typeResourceId = R.string.collection_filter_type_geek_ranking

    override fun inflate(data: String) {
        val d = data.split(DELIMITER.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        min = d.getOrNull(0)?.toIntOrNull()?.clamp(MIN_RANGE, MAX_RANGE) ?: MIN_RANGE
        max = d.getOrNull(1)?.toIntOrNull()?.clamp(MIN_RANGE, MAX_RANGE) ?: MAX_RANGE
        includeUnranked = d.getOrNull(2) == "1"
    }

    override fun deflate(): String {
        return "$min$DELIMITER$max$DELIMITER${if (includeUnranked) "1" else "0"}"
    }

    override fun toShortDescription(): String {
        return describeRange(R.string.unranked_abbr)
    }

    override fun toLongDescription(): String {
        return context.getString(R.string.ranked) + " " + describeRange(R.string.unranked)
    }

    private fun describeRange(@StringRes unrankedResId: Int): String {
        var text: String = when {
            min >= MAX_RANGE -> String.format(Locale.getDefault(), "%,d", MAX_RANGE) + "+"
            min == max -> String.format(Locale.getDefault(), "%,d", max)
            else -> String.format(Locale.getDefault(), "%,d-%,d", min, max)
        }
        if (includeUnranked) text += String.format(" (+%s)", context.getString(unrankedResId))
        return "#$text"
    }

    override fun getSelection(): String {
        var format = when (min) {
            max -> "%1\$s=?"
            else -> "(%1\$s>=? AND %1\$s<=?)"
        }
        if (includeUnranked) format += " OR %1\$s=0 OR %1\$s IS NULL"
        return String.format(format, BggContract.Collection.GAME_RANK)
    }

    override fun getSelectionArgs(): Array<String>? {
        return when {
            min >= MAX_RANGE -> arrayOf(MAX_RANGE.toString())
            else -> arrayOf(min.toString(), max.toString())
        }
    }

    companion object {
        const val MIN_RANGE = 1
        const val MAX_RANGE = 2000
    }
}
