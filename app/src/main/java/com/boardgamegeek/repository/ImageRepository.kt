package com.boardgamegeek.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.WorkerThread
import com.boardgamegeek.R
import com.boardgamegeek.db.ImageDao
import com.boardgamegeek.io.GeekdoApi
import com.boardgamegeek.provider.BggContract.Companion.PATH_THUMBNAILS
import com.boardgamegeek.util.FileUtils
import com.boardgamegeek.util.RemoteConfig
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ImageRepository(
    val context: Context,
    private val geekdoApi: GeekdoApi,
) {
    private val imageDao = ImageDao(context)

    /** Get a bitmap for the given URL, either from disk or from the network. */
    @WorkerThread
    fun fetchThumbnail(thumbnailUrl: String?): Bitmap? {
        if (thumbnailUrl == null) return null
        if (thumbnailUrl.isBlank()) return null
        val file = getThumbnailFile(context, thumbnailUrl)
        val bitmap: Bitmap? = if (file?.exists() == true) {
            BitmapFactory.decodeFile(file.absolutePath)
        } else {
            try {
                Picasso.with(context)
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

    enum class ImageType {
        THUMBNAIL,
        HERO,
    }

    /***
     * Based on the image ID, returns a list of URLs that could be a valid thumbnail or hero image, in order of validity.
     */
    suspend fun getImageUrls(imageId: Int): Map<ImageType, List<String>> {
        val fallbackList = if (imageId > 0) {
            val imageUrlPrefix = "https://cf.geekdo-images.com/images/pic"
            listOf("$imageUrlPrefix$this.jpg", "$imageUrlPrefix$this.png")
        } else emptyList()

        val urls = fetchImageUrls(imageId)
        val list = urls.mapValues { it.value + fallbackList }
        Timber.d(list.toString())
        return list
    }

    suspend fun fetchImageUrls(imageId: Int): Map<ImageType, List<String>> = withContext(Dispatchers.IO) {
        if (imageId > 0 && RemoteConfig.getBoolean(RemoteConfig.KEY_FETCH_IMAGE_WITH_API)) {
            try {
                val response = geekdoApi.image(imageId)
                mutableMapOf<ImageType, List<String>>().apply {
                    this[ImageType.THUMBNAIL] = listOf(response.images.small.url)
                    this[ImageType.HERO] = listOf(response.images.medium.url, response.images.small.url)
                }
            } catch (e: Exception) {
                Timber.w(e, "Couldn't resolve image ID $imageId")
            }
        }
        emptyMap()
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
