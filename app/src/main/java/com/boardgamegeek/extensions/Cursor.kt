@file:Suppress("NOTHING_TO_INLINE")

package com.boardgamegeek.extensions

import android.database.Cursor
import timber.log.Timber
import java.text.*
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
    return parseDate(getStringOrNull(columnName), apiDateFormat)
}

fun Cursor.getDateInMillis(columnName: String): Long {
    val date = getStringOrNull(columnName) ?: ""
    if (date.isNotEmpty()) {
        val calendar = getCalendar(date)
        return calendar.timeInMillis
    }
    return 0L
}

private fun getCalendar(date: String): Calendar {
    Timber.v("Getting date from string: %s", date)
    val dateParts = date.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    val year = Integer.parseInt(dateParts[0])
    val month = Integer.parseInt(dateParts[1]) - 1
    val day = Integer.parseInt(dateParts[2])
    val calendar = Calendar.getInstance()
    calendar.set(year, month, day)
    return calendar
}

private fun parseDate(date: String?, format: DateFormat): Long {
    return if (date.isNullOrBlank()) {
        0L
    } else {
        try {
            format.parse(date).time
        } catch (e: ParseException) {
            Timber.w(e, "Unable to parse %s as %s", date, format)
            0L
        } catch (e: ArrayIndexOutOfBoundsException) {
            Timber.w(e, "Unable to parse %s as %s", date, format)
            0L
        }
    }
}

inline fun String.whereZeroOrNull() = "($this=0 OR $this IS NULL)"

inline fun String.whereEqualsOrNull() = "($this=? OR $this IS NULL)"

inline fun String.whereNotEqualsOrNull() = "($this!=? OR $this IS NULL)"

/**
 * Fix for Cursor not implementing Closeable until API level 16.
 */
inline fun <T : Cursor?, R> T.use(block: (T) -> R): R {
    var exception: Throwable? = null
    try {
        return block(this)
    } catch (e: Throwable) {
        exception = e
        throw e
    } finally {
        when {
            this == null -> {
            }
            exception == null -> close()
            else ->
                try {
                    close()
                } catch (closeException: Throwable) {
                    // exception.addSuppressed(closeException) - available in API 19
                }
        }
    }
}

inline fun Cursor.getDoubleOrZero(columnName: String) =
        getColumnIndexOrThrow(columnName).let { if (isNull(it)) 0.0 else getDouble(it) }

inline fun Cursor.getIntOrZero(columnName: String) =
        getColumnIndexOrThrow(columnName).let { if (isNull(it)) 0 else getInt(it) }

inline fun Cursor.getLongOrZero(columnName: String) =
        getColumnIndexOrThrow(columnName).let { if (isNull(it)) 0L else getLong(it) }

inline fun Cursor.getBoolean(columnName: String) =
        getColumnIndexOrThrow(columnName).let { if (isNull(it)) false else getInt(it) == 1 }

inline fun Cursor.getStringOrEmpty(columnName: String) =
        getColumnIndexOrThrow(columnName).let { if (isNull(it)) "" else getString(it) ?: "" }

// Below is copied from KTX. Replace with library once it's released

inline fun Cursor.getBlob(columnName: String): ByteArray =
        getBlob(getColumnIndexOrThrow(columnName))

inline fun Cursor.getDouble(columnName: String): Double =
        getDouble(getColumnIndexOrThrow(columnName))

inline fun Cursor.getFloat(columnName: String): Float = getFloat(getColumnIndexOrThrow(columnName))

inline fun Cursor.getInt(columnName: String): Int = getInt(getColumnIndexOrThrow(columnName))

inline fun Cursor.getLong(columnName: String): Long = getLong(getColumnIndexOrThrow(columnName))

inline fun Cursor.getShort(columnName: String): Short = getShort(getColumnIndexOrThrow(columnName))

inline fun Cursor.getString(columnName: String): String =
        getString(getColumnIndexOrThrow(columnName))

inline fun Cursor.getBlobOrNull(index: Int) = if (isNull(index)) null else getBlob(index)

inline fun Cursor.getDoubleOrNull(index: Int) = if (isNull(index)) null else getDouble(index)

inline fun Cursor.getFloatOrNull(index: Int) = if (isNull(index)) null else getFloat(index)

inline fun Cursor.getIntOrNull(index: Int) = if (isNull(index)) null else getInt(index)

inline fun Cursor.getLongOrNull(index: Int) = if (isNull(index)) null else getLong(index)

inline fun Cursor.getShortOrNull(index: Int) = if (isNull(index)) null else getShort(index)

inline fun Cursor.getStringOrNull(index: Int) = if (isNull(index)) null else getString(index)

inline fun Cursor.getBlobOrNull(columnName: String) =
        getColumnIndexOrThrow(columnName).let { if (isNull(it)) null else getBlob(it) }

inline fun Cursor.getDoubleOrNull(columnName: String) =
        getColumnIndexOrThrow(columnName).let { if (isNull(it)) null else getDouble(it) }

inline fun Cursor.getFloatOrNull(columnName: String) =
        getColumnIndexOrThrow(columnName).let { if (isNull(it)) null else getFloat(it) }

inline fun Cursor.getIntOrNull(columnName: String) =
        getColumnIndexOrThrow(columnName).let { if (isNull(it)) null else getInt(it) }

inline fun Cursor.getLongOrNull(columnName: String) =
        getColumnIndexOrThrow(columnName).let { if (isNull(it)) null else getLong(it) }

inline fun Cursor.getShortOrNull(columnName: String) =
        getColumnIndexOrThrow(columnName).let { if (isNull(it)) null else getShort(it) }

inline fun Cursor.getStringOrNull(columnName: String) =
        getColumnIndexOrThrow(columnName).let { if (isNull(it)) null else getString(it) }
