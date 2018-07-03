package com.boardgamegeek.filterer

import android.content.Context
import android.support.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.clamp
import com.boardgamegeek.provider.BggContract.Games
import java.util.*

class PlayTimeFilterer(context: Context) : CollectionFilterer(context) {
    var min = MIN_RANGE
    var max = MAX_RANGE
    var includeUndefined = false

    override val typeResourceId = R.string.collection_filter_type_play_time

    override fun setData(data: String) {
        val d = data.split(DELIMITER.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        min = d.getOrNull(0)?.toIntOrNull()?.clamp(MIN_RANGE, MAX_RANGE) ?: MIN_RANGE
        max = d.getOrNull(1)?.toIntOrNull()?.clamp(MIN_RANGE, MAX_RANGE) ?: MAX_RANGE
        includeUndefined = d.getOrNull(2) == "1"
    }

    override fun flatten(): String {
        return "$min$DELIMITER$max$DELIMITER${if (includeUndefined) "1" else "0"}"
    }

    override fun getDisplayText() = describe(R.string.unknown_abbr)

    override fun getDescription() = describe(R.string.unknown)

    private fun describe(@StringRes unknownResId: Int): String {
        var text = when {
            max == MAX_RANGE -> context.getString(R.string.and_up_suffix_abbr, min)
            min == max -> String.format(Locale.getDefault(), "%,d", max)
            else -> String.format(Locale.getDefault(), "%,d-%,d", min, max)
        }
        text += " " + context.getString(R.string.minutes_abbr)
        if (includeUndefined) text += String.format(" (+%s)", context.getString(unknownResId))
        return text
    }


    override fun getSelection(): String {
        var format = when (max) {
            MAX_RANGE -> "(%1\$s>=?)"
            else -> "(%1\$s>=? AND %1\$s<=?)"
        }
        if (includeUndefined) format += " OR %1\$s IS NULL"
        return String.format(format, Games.PLAYING_TIME)
    }

    override fun getSelectionArgs(): Array<String>? {
        return when (max) {
            MAX_RANGE -> arrayOf(min.toString())
            else -> arrayOf(min.toString(), max.toString())
        }
    }

    companion object {
        const val MIN_RANGE = 0
        const val MAX_RANGE = 300
    }
}
