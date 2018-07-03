package com.boardgamegeek.filterer

import android.content.Context
import android.support.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.clamp
import com.boardgamegeek.provider.BggContract.Games
import java.util.*

class AverageWeightFilterer(context: Context) : CollectionFilterer(context) {
    var min: Double = MIN_RANGE
    var max: Double = MAX_RANGE
    var includeUndefined: Boolean = false

    override val typeResourceId = R.string.collection_filter_type_average_weight

    override fun setData(data: String) {
        val d = data.split(DELIMITER.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        min = d.getOrNull(0)?.toDoubleOrNull()?.clamp(MIN_RANGE, MAX_RANGE) ?: MIN_RANGE
        max = d.getOrNull(1)?.toDoubleOrNull()?.clamp(MIN_RANGE, MAX_RANGE) ?: MAX_RANGE
        includeUndefined = d.getOrNull(2) == "1"
    }

    override fun flatten() = "$min$DELIMITER$max$DELIMITER${if (includeUndefined) "1" else "0"}"

    override fun getDisplayText() =
            context.getString(R.string.weight) + " " + describeRange(R.string.undefined_abbr)

    override fun getDescription() =
            context.getString(R.string.average_weight) + " " + describeRange(R.string.undefined)

    private fun describeRange(@StringRes unratedResId: Int): String {
        var text = if (min == max)
            String.format(Locale.getDefault(), "%.1f", max)
        else
            String.format(Locale.getDefault(), "%.1f-%.1f", min, max)
        if (includeUndefined) text += " (+${context.getString(unratedResId)})"
        return text
    }

    override fun getSelection(): String {
        var format = if (min == max) "%1\$s=?" else "(%1\$s>=? AND %1\$s<=?)"
        if (includeUndefined) format += " OR %1\$s=0 OR %1\$s IS NULL"
        return String.format(format, Games.STATS_AVERAGE_WEIGHT)
    }

    override fun getSelectionArgs() = if (min == max)
        arrayOf(max.toString())
    else
        arrayOf(min.toString(), max.toString())

    companion object {
        const val MIN_RANGE = 1.0
        const val MAX_RANGE = 5.0
    }
}
