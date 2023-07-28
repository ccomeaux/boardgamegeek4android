package com.boardgamegeek.extensions

import android.content.ContentProviderOperation
import android.content.ContentProviderResult
import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.BaseColumns
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import com.boardgamegeek.provider.BggContract
import timber.log.Timber

@Suppress("NOTHING_TO_INLINE")
inline fun ContentResolver.load(
    uri: Uri,
    projection: Array<String>? = null,
    selection: String? = null,
    selectionArgs: Array<String>? = null,
    sortOrder: String? = null,
): Cursor? {
    return this.query(uri, projection, selection, selectionArgs, sortOrder)
}

suspend fun <T> ContentResolver.loadEntity(
    uri: Uri,
    projection: Array<String>? = null,
    selection: String? = null,
    selectionArgs: Array<String>? = null,
    sortOrder: String? = null,
    populateEntity: suspend (cursor: Cursor) -> T?,
): T? {
    this.query(uri, projection, selection, selectionArgs, sortOrder)?.use {
        return if (it.moveToFirst()) {
            populateEntity(it)
        } else null
    }
    return null
}

suspend fun <T> ContentResolver.loadList(
    uri: Uri,
    projection: Array<String>? = null,
    selection: String? = null,
    selectionArgs: Array<String>? = null,
    sortOrder: String? = null,
    populateEntity: suspend (cursor: Cursor) -> T?,
): List<T> {
    val list = mutableListOf<T>()
    this.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
        if (cursor.moveToFirst()) {
            do {
                populateEntity(cursor)?.let { list += it }
            } while (cursor.moveToNext())
        }
    }
    return list
}

fun ContentResolver.applyBatch(batch: ArrayList<ContentProviderOperation>?, debugMessage: String = ""): Array<ContentProviderResult> {
    if (batch != null && batch.size > 0) {
        try {
            return applyBatch(BggContract.CONTENT_AUTHORITY, batch)
        } catch (e: Exception) {
            val m = "Applying batch: $debugMessage"
            Timber.e(e, m)
            throw RuntimeException(m, e)
        }
    }
    return arrayOf()
}

fun ContentResolver.rowExists(uri: Uri): Boolean {
    return getCount(uri) > 0
}

fun ContentResolver.getCount(uri: Uri): Int {
    query(uri, arrayOf(BaseColumns._ID), null, null, null)?.use {
        if (it.moveToFirst()) return it.count
    }
    return 0
}

fun ContentResolver.queryString(
    uri: Uri,
    columnName: String
): String? {
    query(uri, arrayOf(columnName), null, null, null)?.use {
        if (it.count == 1 && it.moveToFirst()) {
            return it.getString(0)
        }
    }
    return null
}

fun ContentResolver.queryStrings(
    uri: Uri,
    columnName: String,
    selection: String? = null,
    selectionArgs: Array<String>? = null,
    sortOrder: String? = null
): List<String> {
    val list = mutableListOf<String>()
    query(uri, arrayOf(columnName), selection, selectionArgs, sortOrder)?.use {
        if (it.moveToFirst()) {
            do {
                list += it.getStringOrNull(0).orEmpty()
            } while (it.moveToNext())
        }
    }
    return list
}

fun ContentResolver.queryInts(
    uri: Uri,
    columnName: String,
    selection: String? = null,
    selectionArgs: Array<String>? = null,
    sortOrder: String? = null,
    valueIfNull: Int = 0,
): List<Int> {
    val list = arrayListOf<Int>()
    query(uri, arrayOf(columnName), selection, selectionArgs, sortOrder)?.use {
        while (it.moveToNext()) {
            list.add(it.getIntOrNull(0) ?: valueIfNull)
        }
    }
    return list
}

fun ContentResolver.queryInt(
    uri: Uri,
    columnName: String,
    defaultValue: Int = 0,
    selection: String? = null,
    selectionArgs: Array<String>? = null,
    sortOrder: String? = null
): Int {
    query(uri, arrayOf(columnName), selection, selectionArgs, sortOrder)?.use {
        if (it.count != 1) return defaultValue
        if (it.moveToFirst()) return it.getInt(0)
    }
    return defaultValue
}

fun ContentResolver.queryLong(
    uri: Uri,
    columnName: String,
    defaultValue: Long = 0,
    selection: String? = null,
    selectionArgs: Array<String>? = null,
    sortOrder: String? = null
): Long {
    query(uri, arrayOf(columnName), selection, selectionArgs, sortOrder)?.use {
        if (it.count != 1) return defaultValue
        if (it.moveToFirst()) return it.getLong(0)
    }
    return defaultValue
}
