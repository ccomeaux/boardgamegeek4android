package com.boardgamegeek.util

import android.graphics.drawable.BitmapDrawable
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.palette.graphics.Palette
import com.boardgamegeek.R
import com.boardgamegeek.extensions.ensureHttpsScheme
import com.boardgamegeek.io.Adapter
import com.squareup.picasso.Picasso
import timber.log.Timber
import java.lang.Exception
import java.util.*

/**
 * Loading images? This is your huckleberry.
 */
object ImageUtils {
    /**
     * Call back from loading an image.
     */
    interface Callback {
        fun onSuccessfulImageLoad(palette: Palette?)

        fun onFailedImageLoad()
    }

    private const val IMAGE_URL_PREFIX = "https://cf.geekdo-images.com/images/pic"

    /**
     * Loads an image into the [android.widget.ImageView] by attempting various sizes and image formats. Applies
     * fit/center crop and will load a [androidx.palette.graphics.Palette].
     */
    suspend fun ImageView.safelyLoadImage(imageId: Int, callback: Callback? = null) {
        RemoteConfig.fetch()
        if (RemoteConfig.getBoolean(RemoteConfig.KEY_FETCH_IMAGE_WITH_API)) {
            try {
                val response = Adapter.createGeekdoApi().image(imageId)
                val queue = LinkedList<String>()
                queue.add(response.images.medium.url)
                queue.add(response.images.small.url)
                addDefaultImagesToQueue(imageId, queue)
                safelyLoadImage(queue, callback)
            } catch (e: Exception) {
                safelyLoadImage(addDefaultImagesToQueue(imageId), callback)
            }
        } else {
            safelyLoadImage(addDefaultImagesToQueue(imageId), callback)
        }
    }

    /**
     * Loads an image into the [android.widget.ImageView] by attempting various sizes. Applies fit/center crop and
     * will load a [androidx.palette.graphics.Palette].
     */
    suspend fun ImageView.safelyLoadImage(imageUrl: String, thumbnailUrl: String, heroImageUrl: String? = "", callback: Callback? = null) {
        RemoteConfig.fetch()
        if (heroImageUrl?.isNotEmpty() == true) {
            val queue = LinkedList<String>()
            queue.add(heroImageUrl)
            loadImages(thumbnailUrl, imageUrl, callback, queue)
        } else if (RemoteConfig.getBoolean(RemoteConfig.KEY_FETCH_IMAGE_WITH_API)) {
            val imageId = imageUrl.getImageId()
            if (imageId > 0) {
                try {
                    val response = Adapter.createGeekdoApi().image(imageId)
                    val queue = LinkedList<String>().apply {
                        add(response.images.medium.url)
                        add(response.images.small.url)
                    }
                    loadImages(thumbnailUrl, imageUrl, callback, queue)
                } catch (e: Exception) {
                    loadImages(thumbnailUrl, imageUrl, callback)

                }
            } else {
                loadImages(thumbnailUrl, imageUrl, callback)
            }
        } else {
            loadImages(thumbnailUrl, imageUrl, callback)
        }
    }

    private fun ImageView.loadImages(thumbnailUrl: String, imageUrl: String, callback: Callback?, q: Queue<String>? = null) {
        var queue = q
        if (queue == null) queue = LinkedList()
        queue.add(thumbnailUrl)
        queue.add(imageUrl)
        safelyLoadImage(queue, callback)
    }

    fun String.getImageId(): Int {
        if (isBlank()) return 0

        val picRegex = "/pic\\d+.".toRegex()
        val picMatch = picRegex.find(this)
        if (picMatch != null) {
            return getNumberFromString(picMatch.value)
        }

        val avatarRegex = "/avatar_\\d+.".toRegex()
        val avatarMatch = avatarRegex.find(this)
        if (avatarMatch != null) {
            return getNumberFromString(avatarMatch.value)
        }

        return 0
    }

    private fun getNumberFromString(stringValue: String): Int {
        val numberRegex = "\\d+".toRegex()
        val intValue = numberRegex.find(stringValue)?.value
        return try {
            return intValue?.toInt() ?: 0
        } catch (e: NumberFormatException) {
            Timber.w("Didn't find an image ID in the URL [$stringValue]")
            0
        }
    }

    suspend fun ImageView.loadThumbnail(imageId: Int) {
        if (imageId <= 0) {
            Timber.i("Not attempting to fetch invalid image ID of 0 or negative [%s].", imageId)
            return
        }
        RemoteConfig.fetch()
        if (RemoteConfig.getBoolean(RemoteConfig.KEY_FETCH_IMAGE_WITH_API)) {
            try {
                val response = Adapter.createGeekdoApi().image(imageId)
                val queue = LinkedList<String>().apply { add(response.images.small.url) }
                addDefaultImagesToQueue(imageId, queue)
                safelyLoadThumbnail(queue)
            } catch (e: Exception) {
                safelyLoadThumbnail(addDefaultImagesToQueue(imageId))
            }
        } else {
            safelyLoadThumbnail(addDefaultImagesToQueue(imageId))
        }
    }

    private fun addDefaultImagesToQueue(imageId: Int, q: Queue<String>? = null): Queue<String> {
        return (q ?: LinkedList()).apply {
            add("$IMAGE_URL_PREFIX$imageId.jpg")
            add("$IMAGE_URL_PREFIX$imageId.png")
        }
    }

    fun ImageView.loadThumbnail(vararg paths: String) {
        val queue = LinkedList<String>()
        queue += paths
        safelyLoadThumbnail(queue)
    }

    private fun ImageView.safelyLoadThumbnail(imageUrls: Queue<String>, @DrawableRes errorResId: Int = R.drawable.thumbnail_image_empty) {
        var url: String? = null
        while (url.isNullOrEmpty() && imageUrls.isNotEmpty()) {
            url = imageUrls.poll()
        }
        val imageUrl = url
        Picasso.with(context)
            .load(imageUrl.ensureHttpsScheme())
            .placeholder(errorResId)
            .error(errorResId)
            .fit()
            .centerCrop()
            .into(this, object : com.squareup.picasso.Callback {
                override fun onSuccess() {
                }

                override fun onError() {
                    safelyLoadThumbnail(imageUrls, errorResId)
                }
            })
    }

    /**
     * Loads an image into the [android.widget.ImageView] by attempting each URL in the [java.util.Queue]
     * until one is successful. Applies fit/center crop and will load a [androidx.palette.graphics.Palette].
     */
    private fun ImageView.safelyLoadImage(imageUrls: Queue<String>, callback: Callback?) {
        var url: String? = null
        val savedUrl = getTag(R.id.url) as String?
        if (savedUrl?.isNotEmpty() == true) {
            if (imageUrls.contains(savedUrl)) {
                url = savedUrl
            } else {
                setTag(R.id.url, null)
            }
        }
        if (url.isNullOrEmpty()) {
            url = imageUrls.poll()
        }
        if (url.isNullOrEmpty()) {
            callback?.onFailedImageLoad()
            return
        }
        val imageUrl = url
        Picasso.with(context)
            .load(imageUrl.ensureHttpsScheme())
            .transform(PaletteTransformation.instance())
            .into(this, object : com.squareup.picasso.Callback {
                override fun onSuccess() {
                    setTag(R.id.url, imageUrl)
                    if (callback != null) {
                        val bitmap = (drawable as BitmapDrawable).bitmap
                        val palette = PaletteTransformation.getPalette(bitmap)
                        callback.onSuccessfulImageLoad(palette)
                    }
                }

                override fun onError() {
                    safelyLoadImage(imageUrls, callback)
                }
            })
    }
}
