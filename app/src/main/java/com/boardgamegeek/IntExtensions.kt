package com.boardgamegeek

import android.content.Context
import com.boardgamegeek.entities.RANK_UNKNOWN
import com.boardgamegeek.entities.YEAR_UNKNOWN
import com.boardgamegeek.io.BggService
import com.boardgamegeek.util.PresentationUtils.getText

fun Int.clamp(min: Int, max: Int) = Math.max(min, Math.min(max, this))

/**
 * Gets the ordinal (1st) for the given cardinal (1)
 */
fun Int.toOrdinal(): String {
    if (this < 0) return "-th"

    val c = toString()
    val onesPlace = c.substring(c.length - 1)
    val tensPlace = if (c.length > 1) {
        c.substring(c.length - 2, c.length - 1)
    } else {
        "0"
    }
    return when {
        tensPlace == "1" -> c + "th"
        onesPlace == "1" -> c + "st"
        onesPlace == "2" -> c + "nd"
        onesPlace == "3" -> c + "rd"
        else -> c + "th"
    }
}

fun Int.asYear(context: Context?): String {
    return when {
        context == null -> this.toString()
        this == YEAR_UNKNOWN -> context.getString(R.string.year_zero)
        this > 0 -> context.getString(R.string.year_positive, this.toString())
        else -> context.getString(R.string.year_negative, (-this).toString())
    }
}

fun Int.asAge(context: Context?): CharSequence {
    return when {
        context == null -> this.toString()
        this <= 0 -> context.getString(R.string.ages_unknown)
        else -> context.getText(R.string.age_prefix, this)
    }
}

fun Int.asWishListPriority(context: Context?): String {
    if (context == null) return ""
    return context.resources.getStringArray(R.array.wishlist_priority).getOrElse(this) { context.getString(R.string.wishlist) }
}

fun Int.isRankValid(): Boolean {
    return this != RANK_UNKNOWN
}

fun Int.asRank(context: Context, name: String, type: String = BggService.RANK_TYPE_SUBTYPE): CharSequence {
    val subtype = name.asRankDescription(context, type)
    return when {
        isRankValid() -> getText(context, R.string.rank_description, this, subtype)
        else -> subtype
    }
}

fun Pair<Int, Int>.asRange(errorText: String = "?"): String {
    return when {
        first == 0 && second == 0 -> errorText
        first == 0 -> "%,d".format(second)
        second == 0 -> "%,d".format(first)
        first == second -> "%,d".format(first)
        else -> "%,d - %,d".format(first, second)
    }
}

/**
 * Format a list of integers as a range.
 * E.g. [1,3,4,5] would return "1, 3 - 5"
 * Assumes list is already sorted
 */
fun List<Int>?.asRange(comma: String = ", ", dash: String = " - ", max: Int = Int.MAX_VALUE): String {
    when {
        this == null -> return ""
        isEmpty() -> return ""
        size == 1 -> return this[0].toString()
        else -> {
            val invalid = -1
            var first = invalid
            var last = invalid
            val sb = StringBuilder()
            for (i in indices) {
                val current = this[i]
                when {
                    current == max -> {
                        if (sb.isNotEmpty()) sb.append(comma)
                        sb.append(first).append("+")
                        first = invalid
                        last = invalid
                    }
                    first == invalid -> first = current
                    current - 1 == this[i - 1] -> last = current
                    last != invalid -> {
                        if (sb.isNotEmpty()) sb.append(comma)
                        sb.append(first).append(dash).append(last)
                        first = invalid
                        last = invalid
                    }
                    else -> {
                        if (sb.isNotEmpty()) sb.append(comma)
                        sb.append(first)
                        first = current
                        last = invalid
                    }
                }
            }
            if (first != invalid) {
                if (last != invalid) {
                    if (sb.isNotEmpty()) sb.append(comma)
                    sb.append(first).append(dash).append(last)
                } else {
                    if (sb.isNotEmpty()) sb.append(comma)
                    sb.append(first)
                }
            }
            return sb.toString()
        }
    }
}
