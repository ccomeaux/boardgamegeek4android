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
    var min by DoubleIntervalDelegate(LOWER_BOUND, LOWER_BOUND, UPPER_BOUND)
    var max by DoubleIntervalDelegate(UPPER_BOUND, LOWER_BOUND, UPPER_BOUND)
    var includeUndefined = false
    var ignoreRange = false

    abstract val columnName: String

    override fun inflate(data: String) {
        data.split(DELIMITER).run {
            min = getOrNull(0)?.toDoubleOrNull() ?: LOWER_BOUND
            max = getOrNull(1)?.toDoubleOrNull() ?: UPPER_BOUND
            includeUndefined = getOrNull(2) == "1"
            ignoreRange = getOrNull(3) == "1"
        }
    }

    override fun deflate() = "$min$DELIMITER$max$DELIMITER${if (includeUndefined) "1" else "0"}$DELIMITER${if (ignoreRange) "1" else "0"}"

    protected fun describe(@StringRes unratedResId: Int, @StringRes prefixResId: Int = -1): String {
        val range = when {
            ignoreRange -> ""
            max == LOWER_BOUND -> formatRating(max)
            min == UPPER_BOUND -> formatRating(min)
            min == LOWER_BOUND -> formatRating(max).andLess()
            max == UPPER_BOUND -> formatRating(min).andMore()
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
        const val LOWER_BOUND = 1.0
        const val UPPER_BOUND = 10.0
    }
}
