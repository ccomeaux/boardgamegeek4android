package com.boardgamegeek.filterer

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.extensions.andLess
import com.boardgamegeek.extensions.andMore
import java.util.*

abstract class RatingFilterer(context: Context) : CollectionFilterer(context) {
    var min by DoubleIntervalDelegate(lowerBound, lowerBound, upperBound)
    var max by DoubleIntervalDelegate(upperBound, lowerBound, upperBound)
    var includeUndefined = false
    var ignoreRange = false

    abstract val columnName: String

    override fun inflate(data: String) {
        val d = data.split(DELIMITER)
        min = d.getOrNull(0)?.toDoubleOrNull() ?: lowerBound
        max = d.getOrNull(1)?.toDoubleOrNull() ?: upperBound
        includeUndefined = d.getOrNull(2) == "1"
        ignoreRange = d.getOrNull(3) == "1"
    }

    override fun deflate() = "$min$DELIMITER$max$DELIMITER${if (includeUndefined) "1" else "0"}$DELIMITER${if (ignoreRange) "1" else "0"}"

    protected fun describe(@StringRes prefixResId: Int, @StringRes unratedResId: Int): String {
        var text = when {
            ignoreRange -> ""
            max == lowerBound -> formatRating(max)
            min == upperBound -> formatRating(min)
            min == lowerBound -> formatRating(max).andLess()
            max == upperBound -> formatRating(min).andMore()
            min == max -> formatRating(max)
            else -> String.format(Locale.getDefault(), "%.1f - %.1f", min, max)
        }
        if (includeUndefined) text += " (+${context.getString(unratedResId)})"
        return context.getString(prefixResId) + " " + text
    }

    private fun formatRating(rating: Double) = String.format(Locale.getDefault(), "%.1f", rating) // TODO .asRating()

    fun filter(rating: Double): Boolean {
        return when {
            rating == 0.0 -> includeUndefined
            ignoreRange -> true
            min == max -> rating == min
            else -> rating in min..max
        }
    }

    companion object {
        const val lowerBound = 1.0
        const val upperBound = 10.0
    }
}
