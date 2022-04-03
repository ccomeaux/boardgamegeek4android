package com.boardgamegeek.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.WorkerThread
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.db.ImageDao
import com.boardgamegeek.provider.BggContract.Companion.PATH_THUMBNAILS
import com.boardgamegeek.util.FileUtils
import com.squareup.picasso.Picasso
import timber.log.Timber
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ImageRepository(val application: BggApplication) {
    private val imageDao = ImageDao(application)

    /** Get a bitmap for the given URL, either from disk or from the network. */
    @WorkerThread
    fun fetchThumbnail(thumbnailUrl: String?): Bitmap? {
        if (thumbnailUrl == null) return null
        if (application.applicationContext == null) return null
        if (thumbnailUrl.isBlank()) return null
        val file = getThumbnailFile(application.applicationContext, thumbnailUrl)
        val bitmap: Bitmap? = if (file?.exists() == true) {
            BitmapFactory.decodeFile(file.absolutePath)
        } else {
            try {
                Picasso.with(application.applicationContext)
                    .load(thumbnailUrl)
                    .resizeDimen(R.dimen.shortcut_icon_size, R.dimen.shortcut_icon_size)
                    .centerCrop()
                    .get()
            } catch (e: IOException) {
                Timber.e(e, "Error downloading the thumbnail.")
                null
            }
        }
        if (bitmap != null && file != null) {
            try {
                BufferedOutputStream(FileOutputStream(file)).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                }
            } catch (e: IOException) {
                Timber.e(e, "Error saving the thumbnail file.")
            }
        }
        return bitmap
    }

    suspend fun delete() {
        imageDao.deleteThumbnails()
        imageDao.deleteAvatars()
    }

    private fun getThumbnailFile(context: Context, url: String): File? {
        if (url.isNotBlank()) {
            val filename = FileUtils.getFileNameFromUrl(url)
            return if (filename.isNotBlank())
                File(FileUtils.generateContentPath(context, PATH_THUMBNAILS), filename)
            else null
        }
        return null
    }
}
