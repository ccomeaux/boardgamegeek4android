package com.boardgamegeek.extensions

import android.content.Context
import android.graphics.Color
import androidx.annotation.ArrayRes
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import com.boardgamegeek.R
import java.text.DecimalFormat
import java.text.NumberFormat
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.reflect.KProperty

fun Double.asPersonalRating(context: Context?, @StringRes defaultResId: Int = R.string.unrated_abbr): String {
    return asBoundedRating(context, DecimalFormat("#0.#"), defaultResId)
}

fun Double.asBoundedRating(context: Context?, format: DecimalFormat, @StringRes defaultResId: Int = ResourcesCompat.ID_NULL): String {
    return when {
        this in 1.0..10.0 -> return asScore(context, defaultResId, format)
        defaultResId != ResourcesCompat.ID_NULL && context != null -> context.getString(defaultResId)
        else -> ""
    }
}

fun Double?.asScore(context: Context? = null, @StringRes defaultResId: Int = ResourcesCompat.ID_NULL, format: DecimalFormat = DecimalFormat("#,##0.#")): String {
    return when {
        this != null -> format.format(this)
        defaultResId != ResourcesCompat.ID_NULL && context != null -> context.getString(defaultResId)
        else -> ""
    }
}

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
fun Double.toColor(colors: List<Int>, defaultColor: Int = Color.TRANSPARENT): Int {
    return if (this < 1 || this > colors.size) defaultColor
    else {
        val index = this.toInt()
        val low = colors.getOrNull(index - 1) ?: defaultColor
        val high = colors.getOrNull(index) ?: defaultColor
        low.blendWith(high, index + 1 - this)
    }
}

// exponential distribution cumulative distribution function
fun Double.cdf(lambda: Double): Double {
    return 1.0 - exp(-1.0 * lambda * this)
}

// inverse exponential distribution cumulative distribution function
fun Double.inverseCdf(lambda: Double): Double {
    return -ln(1.0 - this) / lambda
}

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

class DoubleIntervalDelegate(var value: Double, private val minValue: Double, private val maxValue: Double) {
    @SuppressWarnings("unused")
    operator fun getValue(thisRef: Any, property: KProperty<*>): Double {
        return value
    }

    @SuppressWarnings("unused")
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Double) {
        this.value = value.coerceIn(minValue, maxValue)
    }
}
