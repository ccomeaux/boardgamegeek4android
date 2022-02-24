package com.boardgamegeek.filterer

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.extensions.DoubleIntervalDelegate
import java.util.*

class AverageWeightFilterer(context: Context) : CollectionFilterer(context) {
    var min by DoubleIntervalDelegate(lowerBound, lowerBound, upperBound)
    var max by DoubleIntervalDelegate(upperBound, lowerBound, upperBound)
    var includeUndefined = false
    var ignoreRange = false

    override val typeResourceId = R.string.collection_filter_type_average_weight

    override fun inflate(data: String) {
        data.split(DELIMITER).run {
            min = getOrNull(0)?.toDoubleOrNull() ?: lowerBound
            max = getOrNull(1)?.toDoubleOrNull() ?: upperBound
            includeUndefined = getOrNull(2) == "1"
            ignoreRange = getOrNull(3) == "1"
        }
    }

    override fun deflate() = "$min$DELIMITER$max$DELIMITER${if (includeUndefined) "1" else "0"}$DELIMITER${if (ignoreRange) "1" else "0"}"

    override fun toShortDescription() = describe(R.string.weight, R.string.undefined_weight_abbr)

    override fun toLongDescription() = describe(R.string.average_weight, R.string.undefined)

    private fun describe(@StringRes prefixResId: Int, @StringRes unratedResId: Int): String {
        return "${context.getString(prefixResId)} " + when {
            ignoreRange && includeUndefined -> context.getString(unratedResId)
            includeUndefined -> "${describeRange()} (+${context.getString(unratedResId)})"
            else -> describeRange()
        }
    }

    fun describeRange(rangeDelimiter: String = "-") = when {
        ignoreRange -> ""
        min == max -> String.format(Locale.getDefault(), "%.1f", max)
        else -> String.format(Locale.getDefault(), "%.1f$rangeDelimiter%.1f", min, max)
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
