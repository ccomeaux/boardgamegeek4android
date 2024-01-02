package com.boardgamegeek.provider

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.boardgamegeek.extensions.*
import java.io.FileNotFoundException

abstract class BaseProvider {
    abstract val path: String

    open fun getType(uri: Uri): String? {
        throw UnsupportedOperationException("Unknown uri getting type: $uri")
    }

    open fun query(
        db: SQLiteDatabase,
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        throw UnsupportedOperationException("Unknown uri: $uri")
    }

    @Throws(FileNotFoundException::class)
    open fun openFile(context: Context, db: SQLiteDatabase, uri: Uri, mode: String): ParcelFileDescriptor? {
        throw FileNotFoundException("Unknown uri opening file: $uri")
    }
}
