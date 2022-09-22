package com.boardgamegeek.filterer

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.extensions.DoubleIntervalDelegate
import com.boardgamegeek.extensions.andLess
import com.boardgamegeek.extensions.andMore
import com.boardgamegeek.extensions.asPersonalRating
import java.util.*

abstract class RatingFilterer(context: Context) : CollectionFilterer(context) {
    var min by DoubleIntervalDelegate(lowerBound, lowerBound, upperBound)
    var max by DoubleIntervalDelegate(upperBound, lowerBound, upperBound)
    var includeUndefined = false
    var ignoreRange = false

    abstract val columnName: String

    override fun inflate(data: String) {
        data.split(DELIMITER).run {
            min = getOrNull(0)?.toDoubleOrNull() ?: lowerBound
            max = getOrNull(1)?.toDoubleOrNull() ?: upperBound
            includeUndefined = getOrNull(2) == "1"
            ignoreRange = getOrNull(3) == "1"
        }
    }

    override fun deflate() = "$min$DELIMITER$max$DELIMITER${if (includeUndefined) "1" else "0"}$DELIMITER${if (ignoreRange) "1" else "0"}"

    protected fun describe(@StringRes unratedResId: Int, @StringRes prefixResId: Int = -1): String {
        val range = when {
            ignoreRange -> ""
            max == lowerBound -> formatRating(max)
            min == upperBound -> formatRating(min)
            min == lowerBound -> formatRating(max).andLess()
            max == upperBound -> formatRating(min).andMore()
            min == max -> formatRating(max)
            else -> String.format(Locale.getDefault(), "%.1f - %.1f", min, max)
        }
        val prefix = if (prefixResId == -1) "" else "${context.getString(prefixResId)} "
        return prefix + when {
            ignoreRange && includeUndefined -> context.getString(unratedResId)
            includeUndefined -> "$range (+${context.getString(unratedResId)})"
            else -> range
        }
    }

    private fun formatRating(rating: Double) = rating.asPersonalRating(context, R.string.unrated)

    fun filter(rating: Double): Boolean {
        return when {
            rating == 0.0 -> includeUndefined
            ignoreRange -> false
            min == max -> rating == min
            else -> rating in min..max
        }
    }

    companion object {
        const val lowerBound = 1.0
        const val upperBound = 10.0
    }
}
