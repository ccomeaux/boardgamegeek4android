package com.boardgamegeek.provider

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.boardgamegeek.provider.BggContract.PATH_THUMBNAILS
import com.boardgamegeek.util.FileUtils
import timber.log.Timber
import java.io.IOException

class ThumbnailsProvider : BaseProvider() {
    override val path = PATH_THUMBNAILS

    override fun delete(context: Context, db: SQLiteDatabase, uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        return try {
            FileUtils.deleteContents(FileUtils.generateContentPath(context, PATH_THUMBNAILS))
        } catch (e: IOException) {
            Timber.e(e, "Couldn't delete thumbnails")
            0
        }
    }
}