package com.boardgamegeek.db

import android.content.Context
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException

class ImageDao(private val context: Context) {
    suspend fun deleteFile(avatarFileName: String, path: String) = withContext(Dispatchers.IO) {
        val file = if (avatarFileName.isBlank()) null
        else File(FileUtils.generateContentPath(context, path), avatarFileName)
        if (file?.exists() == true) {
            if (file.delete()) 1 else 0
        } else 0
    }

    suspend fun deleteAvatars() = withContext(Dispatchers.IO) {
        try {
            FileUtils.deleteContents(FileUtils.generateContentPath(context, BggContract.PATH_AVATARS))
        } catch (e: IOException) {
            Timber.e(e, "Couldn't delete avatars")
            0
        }
    }

    suspend fun deleteThumbnails() = withContext(Dispatchers.IO) {
        try {
            FileUtils.deleteContents(FileUtils.generateContentPath(context, BggContract.PATH_THUMBNAILS))
        } catch (e: IOException) {
            Timber.e(e, "Couldn't delete thumbnails")
            0
        }
    }
}
