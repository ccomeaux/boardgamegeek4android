package com.boardgamegeek.provider

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.text.TextUtils
import androidx.core.app.NotificationCompat
import com.boardgamegeek.R
import com.boardgamegeek.extensions.KEY_SYNC_NOTIFICATIONS
import com.boardgamegeek.extensions.get
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.util.NotificationUtils
import com.boardgamegeek.util.SelectionBuilder
import java.io.FileNotFoundException

abstract class BaseProvider {
    open fun getType(uri: Uri): String? {
        throw UnsupportedOperationException("Unknown uri getting type: $uri")
    }

    abstract val path: String

    open fun query(resolver: ContentResolver, db: SQLiteDatabase, uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
        val builder = buildExpandedSelection(uri, projection).where(selection, *(selectionArgs.orEmpty()))
        builder.limit(uri.getQueryParameter(BggContract.QUERY_KEY_LIMIT))
        return builder.query(db, projection, getSortOrder(sortOrder))
    }

    protected fun getSortOrder(sortOrder: String?): String? {
        return if (TextUtils.isEmpty(sortOrder)) {
            defaultSortOrder
        } else {
            sortOrder
        }
    }

    protected open val defaultSortOrder: String?
        get() = null

    protected open fun buildExpandedSelection(uri: Uri, projection: Array<String>?): SelectionBuilder {
        return buildExpandedSelection(uri)
    }

    protected open fun buildExpandedSelection(uri: Uri): SelectionBuilder {
        return buildSimpleSelection(uri)
    }

    protected open fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        throw UnsupportedOperationException("Unknown uri: $uri")
    }

    open fun insert(context: Context, db: SQLiteDatabase, uri: Uri, values: ContentValues): Uri? {
        throw UnsupportedOperationException("Unknown uri inserting: $uri")
    }

    fun update(context: Context, db: SQLiteDatabase, uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        val rowCount = buildSimpleSelection(uri).where(selection, *(selectionArgs.orEmpty())).update(db, values)
        if (rowCount > 0) notifyChange(context, uri)
        return rowCount
    }

    open fun delete(context: Context, db: SQLiteDatabase, uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        val rowCount = buildSimpleSelection(uri).where(selection, *(selectionArgs.orEmpty())).delete(db)
        if (rowCount > 0) notifyChange(context, uri)
        return rowCount
    }

    private fun notifyChange(context: Context, uri: Uri) {
        context.contentResolver.notifyChange(uri, null)
    }

    @Throws(FileNotFoundException::class)
    open fun openFile(context: Context, uri: Uri, mode: String): ParcelFileDescriptor? {
        throw FileNotFoundException("Unknown uri opening file: $uri")
    }

    protected fun queryInt(db: SQLiteDatabase, builder: SelectionBuilder, columnName: String, defaultValue: Int = BggContract.INVALID_ID): Int {
        builder.query(db, arrayOf(columnName), null)?.use {
            if (it.moveToFirst()) {
                return it.getInt(0)
            }
        }
        return defaultValue
    }

    protected fun notifyException(context: Context?, e: SQLException) {
        val prefs = context?.preferences()
        if (prefs != null && prefs[KEY_SYNC_NOTIFICATIONS, false] == true) {
            val builder = NotificationUtils
                    .createNotificationBuilder(context, R.string.title_error, NotificationUtils.CHANNEL_ID_ERROR)
                    .setContentText(e.localizedMessage)
                    .setCategory(NotificationCompat.CATEGORY_ERROR)
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(e.toString()).setSummaryText(e.localizedMessage))
            NotificationUtils.notify(context, NotificationUtils.TAG_PROVIDER_ERROR, 0, builder)
        }
    }
}