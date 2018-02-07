package com.boardgamegeek

import android.database.Cursor
import com.boardgamegeek.util.DateTimeUtils
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

fun Cursor.getInt(columnName: String, defaultValue: Int = 0): Int {
    val index = getColumnIndex(columnName)
    return if (index == -1 || index >= columnCount) {
        defaultValue
    } else getInt(index)
}

fun Cursor.getIntAsString(columnName: String, defaultValue: String = "", treatZeroAsNull: Boolean = false): String {
    val index = getColumnIndex(columnName)
    if (index == -1 || index >= columnCount) {
        return defaultValue
    }

    val value = getInt(index)
    return if (treatZeroAsNull && value == 0) {
        defaultValue
    } else NumberFormat.getNumberInstance().format(value)
}

fun Cursor.getLong(columnName: String, defaultValue: Long = 0L): Long {
    val index = getColumnIndex(columnName)
    return if (index == -1 || index >= columnCount) {
        defaultValue
    } else getLong(index)
}

fun Cursor.getDouble(columnName: String, defaultValue: Double = 0.0): Double {
    val index = getColumnIndex(columnName)
    return if (index == -1 || index >= columnCount) {
        defaultValue
    } else getDouble(index)
}

fun Cursor.getDoubleAsString(columnName: String, defaultValue: String, treatZeroAsNull: Boolean = true, format: DecimalFormat = DecimalFormat("#.0")): String {
    val index = getColumnIndex(columnName)
    if (index == -1 || index >= columnCount) {
        return defaultValue
    }

    val value = getDouble(index)
    return if (treatZeroAsNull && value == 0.0) {
        defaultValue
    } else format.format(value)
}

fun Cursor.getString(columnName: String, defaultValue: String = ""): String {
    val index = getColumnIndex(columnName)
    if (index == -1 || index >= columnCount) {
        return defaultValue
    }
    val value = getString(index) ?: ""
    return if (value.isEmpty()) {
        defaultValue
    } else value
}

fun Cursor.getFirstChar(columnName: String): String {
    return getString(columnName, "-").substring(0, 1).toUpperCase(Locale.getDefault())
}

/**
 * Get a date time as an epoch when stored as date from the API.
 */
fun Cursor.getApiTime(columnName: String): Long {
    return DateTimeUtils.tryParseDate(DateTimeUtils.UNPARSED_DATE, getString(columnName), apiDateFormat)
}
