package com.boardgamegeek.filterer

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity
import java.util.*

class AverageWeightFilterer(context: Context) : CollectionFilterer(context) {
    var min by DoubleIntervalDelegate(lowerBound, lowerBound, upperBound)
    var max by DoubleIntervalDelegate(upperBound, lowerBound, upperBound)
    var includeUndefined = false
    var ignoreRange = false // deprecated

    override val typeResourceId = R.string.collection_filter_type_average_weight

    override fun inflate(data: String) {
        val d = data.split(DELIMITER)
        min = d.getOrNull(0)?.toDoubleOrNull() ?: lowerBound
        max = d.getOrNull(1)?.toDoubleOrNull() ?: upperBound
        includeUndefined = d.getOrNull(2) == "1"
    }

    override fun deflate() = "$min$DELIMITER$max$DELIMITER${if (includeUndefined) "1" else "0"}$DELIMITER${if (ignoreRange) "1" else "0"}"

    override fun toShortDescription() = describe(R.string.weight, R.string.undefined_abbr)

    override fun toLongDescription() = describe(R.string.average_weight, R.string.undefined)

    private fun describe(@StringRes prefixResId: Int, @StringRes unratedResId: Int): String {
        var text = when {
            ignoreRange -> ""
            min == max -> String.format(Locale.getDefault(), "%.1f", max)
            else -> String.format(Locale.getDefault(), "%.1f - %.1f", min, max)
        }
        if (includeUndefined) text += " (+${context.getString(unratedResId)})"
        return context.getString(prefixResId) + " " + text
    }

    override fun filter(item: CollectionItemEntity): Boolean {
        return when {
            item.averageWeight == 0.0 -> includeUndefined
            ignoreRange -> true
            min == max -> item.averageWeight == min
            else -> item.averageWeight in min..max
        }
    }

    companion object {
        const val lowerBound = 1.0
        const val upperBound = 5.0
    }
}
