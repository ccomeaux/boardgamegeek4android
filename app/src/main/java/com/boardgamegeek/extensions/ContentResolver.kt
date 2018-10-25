package com.boardgamegeek.extensions

import android.content.*
import android.database.Cursor
import android.net.Uri
import android.os.RemoteException
import android.provider.BaseColumns
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.util.PreferencesUtils
import timber.log.Timber
import java.util.*

@Suppress("NOTHING_TO_INLINE")
inline fun ContentResolver.load(uri: Uri, projection: Array<String>? = null, selection: String? = null, selectionArgs: Array<String>? = null, sortOrder: String? = null): Cursor? {
    return this.query(uri, projection, selection, selectionArgs, sortOrder)
}

fun ContentResolver.applyBatch(context: Context, batch: ArrayList<ContentProviderOperation>?, debugMessage: String = ""): Array<ContentProviderResult> {
    if (batch != null && batch.size > 0) {
        if (PreferencesUtils.getAvoidBatching(context)) {
            val results = arrayOf<ContentProviderResult>()
            for (i in batch.indices) {
                results[i] = applySingle(batch[i], debugMessage)
            }
            return results
        } else {
            try {
                return applyBatch(BggContract.CONTENT_AUTHORITY, batch)
            } catch (e: OperationApplicationException) {
                val m = "Applying batch: $debugMessage"
                Timber.e(e, m)
                throw RuntimeException(m, e)
            } catch (e: RemoteException) {
                val m = "Applying batch: $debugMessage"
                Timber.e(e, m)
                throw RuntimeException(m, e)
            }
        }
    }
    return arrayOf()
}

private fun ContentResolver.applySingle(cpo: ContentProviderOperation, debugMessage: String): ContentProviderResult {
    val batch = ArrayList<ContentProviderOperation>(1)
    batch.add(cpo)
    try {
        val result = applyBatch(BggContract.CONTENT_AUTHORITY, batch)
        if (result.isNotEmpty()) {
            return result[0]
        }
    } catch (e: OperationApplicationException) {
        val m = cpo.toString() + "\n" + debugMessage
        Timber.e(e, m)
        throw RuntimeException(m, e)
    } catch (e: RemoteException) {
        val m = cpo.toString() + "\n" + debugMessage
        Timber.e(e, m)
        throw RuntimeException(m, e)
    }
    return ContentProviderResult(0)
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
        uri: Uri, columnName: String,
        selection: String? = null,
        selectionArgs: Array<String>? = null,
        sortOrder: String? = null
): List<String> {
    val list = arrayListOf<String>()
    query(uri, arrayOf(columnName), selection, selectionArgs, sortOrder)?.use {
        while (it.moveToNext()) {
            list.add(it.getString(0))
        }
    }
    return list
}

fun ContentResolver.queryInts(
        uri: Uri,
        columnName: String,
        selection: String? = null,
        selectionArgs: Array<String>? = null,
        sortOrder: String? = null
): List<Int> {
    val list = arrayListOf<Int>()
    query(uri, arrayOf(columnName), selection, selectionArgs, sortOrder)?.use {
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

fun ContentResolver.queryCount(uri: Uri): Int {
    return queryInt(uri, "count(*) AS count")
}