package com.boardgamegeek

import android.content.ContentResolver
import android.net.Uri

fun ContentResolver.queryStrings(
        uri: Uri, columnName: String,
        selection: String? = null,
        selectionArgs: Array<String>? = null,
        sortOrder: String? = null
): List<String> {
    val list = arrayListOf<String>()
    val cursor = query(uri, arrayOf(columnName), selection, selectionArgs, sortOrder)
    cursor?.use {
        while (it.moveToNext()) {
            list.add(it.getString(0))
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
    val list = arrayListOf<Int>()
    val cursor = query(uri, arrayOf(columnName), selection, selectionArgs, sortOrder)
    cursor?.use {
        while (it.moveToNext()) {
            list.add(it.getInt(0))
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
    cursor?.use {
        val count = it.count
        if (count != 1) return defaultValue
        return if (it.moveToFirst()) {
            it.getInt(0)
        } else {
            defaultValue
        }
    }
    return defaultValue
}

fun ContentResolver.queryCount(uri: Uri): Int {
    return queryInt(uri, "count(*) AS count")
}