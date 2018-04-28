package com.boardgamegeek

import android.content.ContentResolver
import android.net.Uri
import java.util.*

fun ContentResolver.queryStrings(
        uri: Uri, columnName: String,
        selection: String? = null,
        selectionArgs: Array<String>? = null,
        sortOrder: String? = null
): List<String> {
    val list = ArrayList<String>()
    val cursor = query(uri, arrayOf(columnName), selection, selectionArgs, sortOrder)
    cursor?.use { c ->
        while (c.moveToNext()) {
            list.add(c.getString(0))
        }
    }
    return list
}

fun ContentResolver.queryInts(
        uri: Uri, columnName: String,
        selection: String? = null,
        selectionArgs: Array<String>? = null,
        sortOrder: String? = null
): List<Int> {
    val list = ArrayList<Int>()
    val cursor = query(uri, arrayOf(columnName), selection, selectionArgs, sortOrder)
    cursor?.use { c ->
        while (c.moveToNext()) {
            list.add(c.getInt(0))
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
    val cursor = query(uri, arrayOf(columnName), selection, selectionArgs, sortOrder)
    cursor?.use { c ->
        val count = c.count
        if (count != 1) return defaultValue
        c.moveToFirst()
        return cursor.getInt(0)
    }
    return defaultValue
}

fun ContentResolver.queryCount(uri: Uri): Int {
    return queryInt(uri, "count(*) AS count")
}