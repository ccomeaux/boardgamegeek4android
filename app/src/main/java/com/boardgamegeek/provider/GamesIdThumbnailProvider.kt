package com.boardgamegeek.provider

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.boardgamegeek.provider.BggContract.Companion.PATH_GAMES
import com.boardgamegeek.provider.BggContract.Companion.PATH_THUMBNAILS
import com.boardgamegeek.provider.BggContract.Games
import com.boardgamegeek.util.FileUtils
import java.io.File
import java.io.FileNotFoundException

/**
 * content://com.boardgamegeek/games/110308/thumbnails
 */
class GamesIdThumbnailProvider : BaseProvider() {
    override val path = "$PATH_GAMES/#/$PATH_THUMBNAILS"

    @Throws(FileNotFoundException::class)
    override fun openFile(context: Context, uri: Uri, mode: String): ParcelFileDescriptor? {
        val file = getFile(context, uri)
        if (file?.exists() != true) {
            throw FileNotFoundException("Couldn't get the file at the specified path.")
        }
        val parcelMode = calculateParcelMode(uri, mode)
        return ParcelFileDescriptor.open(file, parcelMode)
    }

    /**
     * Get a `File` representing the URI. E.g. content://com.boardgamegeek/games/110308/thumbnails
     * becomes the file from /storage/emulated/0/Android/data/com.boardgamegeek/files/thumbnails/pic1115825.jpg
     */
    private fun getFile(context: Context, uri: Uri): File? {
        val fileName = generateFileName(context, uri)
        return if (fileName.isNullOrEmpty()) null
        else File(FileUtils.generateContentPath(context, PATH_THUMBNAILS), fileName)
    }

    /**
     * Generates a file name based on the URI. E.g. content://com.boardgamegeek/games/110308/thumbnails
     * becomes pic1115825.jpg
     */
    private fun generateFileName(context: Context, uri: Uri): String? {
        return Games.buildGameUri(Games.getGameId(uri))?.let {
            context.contentResolver.query(it, arrayOf(Games.Columns.THUMBNAIL_URL), null, null, null)?.use { cursor ->
                if (cursor.count == 1 && cursor.moveToFirst()) {
                    cursor.getString(0)
                } else {
                    null
                }
            }?.let { thumbnailUrl ->
                FileUtils.getFileNameFromUrl(thumbnailUrl)
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
