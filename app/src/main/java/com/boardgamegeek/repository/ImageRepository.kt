package com.boardgamegeek.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.boardgamegeek.R
import com.boardgamegeek.db.ImageDao
import com.boardgamegeek.io.GeekdoApi
import com.boardgamegeek.io.safeApiCall
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.util.FileUtils
import com.boardgamegeek.util.RemoteConfig
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException

class ImageRepository(
    val context: Context,
    private val geekdoApi: GeekdoApi,
) {
    private val imageDao = ImageDao(context)

    /** Get a bitmap for the given URL, either from disk or from the network. */
    suspend fun fetchThumbnail(thumbnailUrl: String?): Bitmap? = withContext(Dispatchers.IO) {
          FileUtils.getFile(context, BggContract.PATH_THUMBNAILS, thumbnailUrl)?.let { file ->
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                try {
                    Picasso.with(context)
                        .load(thumbnailUrl)
                        .resizeDimen(R.dimen.shortcut_icon_size, R.dimen.shortcut_icon_size)
                        .centerCrop()
                        .get()
                } catch (e: IOException) {
                    Timber.e(e, "Error downloading the thumbnail at $thumbnailUrl.")
                    null
                }?.let { bitmap ->
                    FileUtils.saveBitmap(file, bitmap)
                    bitmap
                }
            }
        }
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
            listOf("$imageUrlPrefix$imageId.jpg", "$imageUrlPrefix$imageId.png")
        } else emptyList()

        val urls = fetchImageUrls(imageId)
        val list = urls.mapValues { it.value + fallbackList }
        Timber.d(list.toString())
        return list
    }

    private suspend fun fetchImageUrls(imageId: Int): Map<ImageType, List<String>> = withContext(Dispatchers.IO) {
        if (imageId > 0 && RemoteConfig.getBoolean(RemoteConfig.KEY_FETCH_IMAGE_WITH_API)) {
            val result = safeApiCall(context) { geekdoApi.image(imageId) }
            if (result.isSuccess) {
                result.getOrNull()?.let {
                    return@withContext mapOf(
                        ImageType.THUMBNAIL to listOf(it.images.small.url),
                        ImageType.HERO to listOf(it.images.medium.url, it.images.small.url),
                    )
                }
            } else {
                result.exceptionOrNull()?.let {
                    Timber.w(it, "Couldn't resolve image ID $imageId")
                }
            }
        }
        emptyMap()
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        imageDao.deleteThumbnails()
        imageDao.deleteAvatars()
    }

    suspend fun deleteThumbnail(fileName: String) = withContext(Dispatchers.IO) {
        imageDao.deleteFile(fileName, BggContract.PATH_THUMBNAILS)
    }
}
