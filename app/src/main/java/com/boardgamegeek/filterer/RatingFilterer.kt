package com.boardgamegeek.filterer

import android.content.Context
import android.support.annotation.StringRes
import java.util.*

abstract class RatingFilterer(context: Context) : CollectionFilterer(context) {
    var min by DoubleIntervalDelegate(lowerBound, lowerBound, upperBound)
    var max by DoubleIntervalDelegate(upperBound, lowerBound, upperBound)
    var includeUndefined = false

    abstract val columnName: String

    override fun inflate(data: String) {
        val d = data.split(DELIMITER.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        min = d.getOrNull(0)?.toDoubleOrNull() ?: lowerBound
        max = d.getOrNull(1)?.toDoubleOrNull() ?: upperBound
        includeUndefined = d.getOrNull(2) == "1"
    }

    override fun deflate() = "$min$DELIMITER$max$DELIMITER${if (includeUndefined) "1" else "0"}"

    protected fun describe(@StringRes prefixResId: Int, @StringRes unratedResId: Int): String {
        var text = when (min) {
            max -> String.format(Locale.getDefault(), "%.1f", max)
            else -> String.format(Locale.getDefault(), "%.1f - %.1f", min, max)
        }
        if (includeUndefined) text += " (+${context.getString(unratedResId)})"
        return context.getString(prefixResId) + " " + text
    }

    override fun getSelection(): String {
        var format = when (min) {
            max -> "%1\$s=?"
            else -> "(%1\$s>=? AND %1\$s<=?)"
        }
        if (includeUndefined) format += " OR %1\$s=0 OR %1\$s IS NULL"
        return String.format(format, columnName)
    }

    override fun getSelectionArgs() = when (min) {
        max -> arrayOf(min.toString())
        else -> arrayOf(min.toString(), max.toString())
    }

    companion object {
        const val lowerBound = 1.0
        const val upperBound = 10.0
    }
}