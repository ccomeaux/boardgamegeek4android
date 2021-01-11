@file:Suppress("NOTHING_TO_INLINE")

package com.boardgamegeek.extensions

import android.database.Cursor

fun Cursor.getInt(columnName: String, defaultValue: Int = 0): Int {
    val index = getColumnIndex(columnName)
    return if (index == -1 || index >= columnCount) {
        defaultValue
    } else getInt(index)
}

fun Cursor.getLong(columnName: String, defaultValue: Long = 0L): Long {
    val index = getColumnIndex(columnName)
    return if (index == -1 || index >= columnCount) {
        defaultValue
    } else getLong(index)
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

inline fun Cursor.getInt(columnName: String): Int = getInt(getColumnIndexOrThrow(columnName))

inline fun Cursor.getLong(columnName: String): Long = getLong(getColumnIndexOrThrow(columnName))

inline fun Cursor.getString(columnName: String): String = getString(getColumnIndexOrThrow(columnName))

inline fun Cursor.getDoubleOrZero(index: Int) = if (isNull(index)) 0.0 else getDouble(index)

inline fun Cursor.getIntOrNull(index: Int) = if (isNull(index)) null else getInt(index)

inline fun Cursor.getIntOrZero(index: Int) = if (isNull(index)) 0 else getInt(index)

inline fun Cursor.getLongOrNull(index: Int) = if (isNull(index)) null else getLong(index)

inline fun Cursor.getLongOrZero(index: Int) = if (isNull(index)) 0L else getLong(index)

inline fun Cursor.getStringOrNull(index: Int) = if (isNull(index)) null else getString(index)

inline fun Cursor.getBoolean(index: Int) = if (isNull(index)) false else getInt(index) == 1

inline fun Cursor.getIntOrNull(columnName: String) =
        getColumnIndexOrThrow(columnName).let { if (isNull(it)) null else getInt(it) }

inline fun Cursor.getLongOrNull(columnName: String) =
        getColumnIndexOrThrow(columnName).let { if (isNull(it)) null else getLong(it) }
