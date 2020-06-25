package com.boardgamegeek.provider

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.boardgamegeek.util.FileUtils
import java.io.File
import java.io.FileNotFoundException

abstract class BaseFileProvider : BaseProvider() {
    @Throws(FileNotFoundException::class)
    override fun openFile(context: Context, uri: Uri, mode: String): ParcelFileDescriptor? {
        val file = getFile(context, uri)
        if (file?.exists() != true) {
            throw FileNotFoundException("Couldn't get the file at the specified path.")
        }
        val parcelMode = calculateParcelMode(uri, mode)
        return ParcelFileDescriptor.open(file, parcelMode)
    }

    override fun delete(context: Context, db: SQLiteDatabase, uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        val file = getFile(context, uri)
        return if (file?.exists() == true) {
            if (file.delete()) 1 else 0
        } else 0
    }

    /**
     * Get a `File` representing the URI. E.g. content://com.boardgamegeek/games/110308/thumbnails
     * becomes the file from /storage/emulated/0/Android/data/com.boardgamegeek/files/thumbnails/pic1115825.jpg
     */
    private fun getFile(context: Context, uri: Uri): File? {
        val fileName = generateFileName(context, uri)
        return if (fileName.isNullOrEmpty()) null
        else File(FileUtils.generateContentPath(context, contentPath), fileName)
    }

    /**
     * Generates a file name based on the URI. E.g. content://com.boardgamegeek/games/110308/thumbnails
     * becomes pic1115825.jpg
     */
    protected abstract fun generateFileName(context: Context, uri: Uri): String?

    protected abstract val contentPath: String

    companion object {
        // from Android ContentResolver.modeToMode
        @Throws(FileNotFoundException::class)
        private fun calculateParcelMode(uri: Uri, mode: String): Int {
            return when (mode) {
                "r" -> ParcelFileDescriptor.MODE_READ_ONLY
                "w", "wt" -> ParcelFileDescriptor.MODE_WRITE_ONLY or ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_TRUNCATE
                "wa" -> ParcelFileDescriptor.MODE_WRITE_ONLY or ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_APPEND
                "rw" -> ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_CREATE
                "rwt" -> ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_TRUNCATE
                "" -> throw FileNotFoundException("Missing mode for $uri")
                else -> throw FileNotFoundException("Bad mode for $uri: $mode")
            }
        }
    }
}