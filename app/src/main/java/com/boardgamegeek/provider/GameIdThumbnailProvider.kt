package com.boardgamegeek.provider

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.boardgamegeek.provider.BggContract.Games
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.provider.BggContract.Companion.PATH_GAMES
import com.boardgamegeek.provider.BggContract.Companion.PATH_THUMBNAILS
import com.boardgamegeek.util.FileUtils
import java.io.File
import java.io.FileNotFoundException

/**
 * content://com.boardgamegeek/collection/123456/thumbnails
 */
class GameIdThumbnailProvider : BaseProvider() {
    override val path = "$PATH_GAMES/#/$PATH_THUMBNAILS"

    @Throws(FileNotFoundException::class)
    override fun openFile(context: Context, db: SQLiteDatabase, uri: Uri, mode: String): ParcelFileDescriptor? {
        val file = getFile(context, db, uri)
        if (file?.exists() != true) {
            throw FileNotFoundException("Couldn't get the file at the specified path.")
        }
        val parcelMode = calculateParcelMode(uri, mode)
        return ParcelFileDescriptor.open(file, parcelMode)
    }

    /**
     * Get a `File` representing the URI. E.g. content://com.boardgamegeek/games/123456/thumbnails
     * becomes the file from /storage/emulated/0/Android/data/com.boardgamegeek/files/thumbnails/pic1115825.jpg
     */
    private fun getFile(context: Context, db: SQLiteDatabase, uri: Uri): File? {
        val fileName = generateFileName(db, uri)
        return FileUtils.getFile(context, PATH_THUMBNAILS, fileName)
    }

    /**
     * Generates a file name based on the URI. E.g. content://com.boardgamegeek/games/110308/thumbnails
     * becomes pic1115825.jpg
     */
    private fun generateFileName(db: SQLiteDatabase, uri: Uri): String? {
        val gameId = Games.getGameId(uri)
        val qb = SQLiteQueryBuilder().apply { tables = BggDatabase.Tables.GAMES }
        return qb.query(
            db,
            arrayOf(Games.Columns.THUMBNAIL_URL),
            "${Collection.Columns.GAME_ID}=?",
            arrayOf(gameId.toString()),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.count == 1 && cursor.moveToFirst()) {
                cursor.getString(0)
            } else {
                null
            }
        }
    }

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
