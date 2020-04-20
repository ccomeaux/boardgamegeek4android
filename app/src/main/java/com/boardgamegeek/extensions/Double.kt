@file:JvmName("DoubleUtils")

package com.boardgamegeek.extensions

import android.content.Context
import android.graphics.Color
import androidx.annotation.ArrayRes
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import com.boardgamegeek.R
import java.text.DecimalFormat
import java.text.NumberFormat
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt

fun Double.asPersonalRating(context: Context?, @StringRes defaultResId: Int = R.string.unrated): String {
    return asScore(context, defaultResId, DecimalFormat("#0.#"))
}

fun Double.asRating(context: Context?, @StringRes defaultResId: Int = R.string.unrated): String {
    return asScore(context, defaultResId)
}

fun Double?.asScore(context: Context?, @StringRes defaultResId: Int = 0, format: DecimalFormat = DecimalFormat("#0.0#")): String {
    return when {
        this != null -> format.format(this)
        defaultResId != 0 && context != null -> context.getString(defaultResId)
        else -> ""
    }
}

@JvmOverloads
fun Double.asPercentage(format: DecimalFormat = DecimalFormat("0.0")): String {
    return format.format(this * 100) + "%"
}

fun Double.toDescription(context: Context, @ArrayRes arrayResId: Int, @StringRes zeroStringResId: Int = R.string.unknown): CharSequence {
    if (this == 0.0) return context.getString(zeroStringResId)
    val array = context.resources.getStringArray(arrayResId)
    val index = this.roundToInt() - 1
    return array.getOrElse(index) { context.getString(zeroStringResId) }
}

@ColorInt
fun Double.toColor(colors: IntArray): Int {
    return if (this < 1 || this > colors.size) Color.TRANSPARENT
    else {
        val index = this.toInt()
        val low = colors.getOrNull(index - 1) ?: Color.TRANSPARENT
        val high = colors.getOrNull(index) ?: Color.TRANSPARENT
        low.blendWith(high, index + 1 - this)
    }
}

fun Double.cdf(lambda: Double): Double {
    return 1.0 - exp(-1.0 * lambda * this)
}

fun Double.invcdf(lambda: Double): Double {
    return -ln(1.0 - this) / lambda
}

@JvmOverloads
fun Double.asMoney(currency: String, format: DecimalFormat = MONEY_FORMAT): String {
    return if (currency.isBlank() && this == 0.0) "" else currency.asCurrency() + format.format(this)
}

private val MONEY_FORMAT: DecimalFormat
    get() = setUpMoneyFormatter()

private fun setUpMoneyFormatter(): DecimalFormat {
    val format = NumberFormat.getCurrencyInstance() as DecimalFormat
    val symbols = format.decimalFormatSymbols
    symbols.currencySymbol = ""
    format.decimalFormatSymbols = symbols
    return format
}