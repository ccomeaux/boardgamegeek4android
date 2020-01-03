@file:JvmName("IntUtils")

package com.boardgamegeek.extensions

import android.content.Context
import android.graphics.Color
import android.text.format.DateUtils
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.entities.RANK_UNKNOWN
import com.boardgamegeek.entities.YEAR_UNKNOWN
import com.boardgamegeek.io.BggService
import java.math.BigDecimal
import java.math.MathContext

/**
 * Gets the ordinal (1st) for the given cardinal (1)
 * Note, this is English-specific
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
        isRankValid() -> context.getText(R.string.rank_description, this, subtype)
        else -> subtype
    }
}

/**
 * Returns
 * 1. the count rounded down
 * 2. The description of the count (if available)
 * 3. The color associated with the description (or transparent)
 */
fun Int.asPlayCount(context: Context): Triple<Int, String, Int> {
    val playCounts = mutableListOf<Triple<Int, Int, String>>()
    playCounts.add(Triple(100, R.string.play_stat_dollar, "#85bb65"))
    playCounts.add(Triple(25, R.string.play_stat_quarter, "#D3D3D3"))
    playCounts.add(Triple(10, R.string.play_stat_dime, "#C0C0C0"))
    playCounts.add(Triple(5, R.string.play_stat_nickel, "#B8B8B6"))
    playCounts.add(Triple(1, R.string.play_stat_penny, "#b87333"))
    val pc = playCounts.find {
        this >= it.first
    } ?: Triple(0, 0, "#00000000")
    return Triple(pc.first, if (pc.second == 0) "" else context.getString(pc.second), Color.parseColor(pc.third))
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
 * Format minutes as "H:MM"
 */
fun Int.asTime(): String {
    if (this > 0) {
        val hours = this / 60
        val minutes = this % 60
        return String.format("%d:%02d", hours, minutes)
    }
    return "0:00"
}

/**
 * Format minutes as 90 mins or 2 hrs or 2h 15m
 */
fun Int.asMinutes(context: Context): String {
    if (this == 0) return context.getString(R.string.mins_unknown)

    return if (this >= 120) {
        val hours = this / 60
        val remainingMinutes = this % 60

        if (remainingMinutes == 0) {
            context.getString(R.string.hrs_suffix, hours)
        } else {
            context.getString(R.string.hrs_mins, hours, remainingMinutes)
        }
    } else {
        context.resources.getQuantityString(R.plurals.mins_suffix, this, this)
    }
}

/**
 * Returns the long representation of the time <code>this</<code> hours ago.
 */
fun Int.hoursAgo(): Long {
    return System.currentTimeMillis() - (this * DateUtils.HOUR_IN_MILLIS)
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

fun Int.asHttpErrorMessage(context: Context): String {
    @StringRes val resId: Int = when {
        this >= 500 -> R.string.msg_sync_response_500
        this == 429 -> R.string.msg_sync_response_429
        this == 202 -> R.string.msg_sync_response_202
        else -> R.string.msg_sync_error_http_code
    }
    return context.getString(resId, this.toString())
}

fun Int.significantDigits(digits: Int): Int {
    return BigDecimal(this).round(MathContext(digits)).toInt()
}

fun Int.orderOfMagnitude(): String {
    val digit = toString().substring(0, 1)
    val suffix = when {
        this >= 1000000000 -> "B"
        this >= 1000000 -> "M"
        this > 1000 -> "K"
        else -> ""
    }
    val zeros = (toString().length - 1) % 3
    val number = digit + ("0".repeat(zeros)) + suffix
    return if (this < 10) number else "$number+"
}
