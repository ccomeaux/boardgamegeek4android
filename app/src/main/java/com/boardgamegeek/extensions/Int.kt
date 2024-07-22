package com.boardgamegeek.extensions

import android.content.Context
import android.graphics.Color
import android.text.format.DateUtils
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.model.Game
import java.math.BigDecimal
import java.math.MathContext
import java.text.NumberFormat
import java.util.Locale
import kotlin.reflect.KProperty

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

fun Int.asRangeDescription(quantity: Int): String {
    return when (quantity) {
        1 -> toOrdinal()
        2 -> "${(this - 1).toOrdinal()} & ${toOrdinal()}"
        else -> "${(this - quantity + 1).toOrdinal()} - ${toOrdinal()}"
    }
}

fun Int.asYear(context: Context?): String {
    return when {
        context == null -> this.toString()
        this == Game.YEAR_UNKNOWN -> context.getString(R.string.year_zero)
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

/**
 * Assumes that both end points are positive/non-zero.
 */
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
        return String.format(Locale.getDefault(), "%d:%02d", hours, minutes)
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
 * Returns the long representation of the time <code>this</code> hours ago.
 */
fun Int.hoursAgo(): Long {
    return System.currentTimeMillis() - (this * DateUtils.HOUR_IN_MILLIS)
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
        this >= 1_000_000_000 -> "B"
        this >= 1_000_000 -> "M"
        this > 1_000 -> "K"
        else -> ""
    }
    val zeros = (toString().length - 1) % 3
    val number = digit + ("0".repeat(zeros)) + suffix
    return if (this < 10) number else "$number+"
}

fun Int.toFormattedString(): String {
    return this.toString().format(NumberFormat.getInstance())
}

@Suppress("unused")
class IntervalDelegate(var value: Int, private val minValue: Int, private val maxValue: Int) {
    operator fun getValue(thisRef: Any, property: KProperty<*>): Int {
        return value
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
        this.value = value.coerceIn(minValue, maxValue)
    }
}
