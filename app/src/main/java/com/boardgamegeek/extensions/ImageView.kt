package com.boardgamegeek.extensions

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.view.setMargins
import androidx.palette.graphics.Palette
import com.boardgamegeek.R
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.util.PaletteTransformation
import com.boardgamegeek.util.RemoteConfig
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import timber.log.Timber
import java.util.*

fun ImageView.setOrClearColorFilter(@ColorInt color: Int) {
    if (color == Color.TRANSPARENT) clearColorFilter() else setColorFilter(color)
}

/**
 * Call back from loading an image.
 */
interface ImageLoadCallback {
    fun onSuccessfulImageLoad(palette: Palette?)

    fun onFailedImageLoad()
}

/**
 * Loads the URL into an ImageView, triggering a callback with the palette for the loaded image. If the URL appears to be for the same image, no
 * placeholder or fade animation is shown.
 */
fun ImageView.loadUrl(url: String?, callback: ImageLoadCallback? = null) {
    val tag = getTag(R.id.image)
    val isSameImage = tag != null && tag == url?.getImageId()
    val requestCreator = Picasso.with(context)
        .load(url.ensureHttpsScheme())
        .transform(PaletteTransformation.instance())
    if (isSameImage) {
        requestCreator.noFade().noPlaceholder()
    }
    requestCreator.into(this, object : Callback {
        override fun onSuccess() {
            setTag(R.id.image, url?.getImageId())
            callback?.onSuccessfulImageLoad(PaletteTransformation.getPalette((drawable as BitmapDrawable).bitmap))
        }

        override fun onError() {
            callback?.onFailedImageLoad()
        }
    })
}

/**
 * Loads an image into the [android.widget.ImageView] by attempting various sizes and image formats. Applies
 * fit/center crop and will load a [androidx.palette.graphics.Palette].
 */
suspend fun ImageView.safelyLoadImage(imageId: Int, callback: ImageLoadCallback? = null) {
    if (imageId <= 0) {
        Timber.i("Not attempting to fetch invalid image ID of 0 or negative [%s].", imageId)
        return
    }
    RemoteConfig.fetch()
    if (RemoteConfig.getBoolean(RemoteConfig.KEY_FETCH_IMAGE_WITH_API)) {
        try {
            val response = Adapter.createGeekdoApi().image(imageId)
            loadImages(callback, response.images.medium.url, response.images.small.url, *imageId.createImageUrls().toTypedArray())
        } catch (e: Exception) {
            loadImages(callback, *imageId.createImageUrls().toTypedArray())
        }
    } else {
        loadImages(callback, *imageId.createImageUrls().toTypedArray())
    }
}

/**
 * Loads an image into the [android.widget.ImageView] by attempting various sizes. Applies fit/center crop and
 * will load a [androidx.palette.graphics.Palette].
 */
suspend fun ImageView.safelyLoadImage(imageUrl: String, thumbnailUrl: String, heroImageUrl: String? = "", callback: ImageLoadCallback? = null) {
    RemoteConfig.fetch()
    if (heroImageUrl?.isNotEmpty() == true) {
        loadImages(callback, heroImageUrl, thumbnailUrl, imageUrl)
    } else if (RemoteConfig.getBoolean(RemoteConfig.KEY_FETCH_IMAGE_WITH_API)) {
        val imageId = imageUrl.getImageId()
        if (imageId > 0) {
            try {
                val response = Adapter.createGeekdoApi().image(imageId)
                loadImages(callback, response.images.medium.url, response.images.small.url, thumbnailUrl, imageUrl)
            } catch (e: Exception) {
                loadImages(callback, thumbnailUrl, imageUrl)
            }
        } else {
            loadImages(callback, thumbnailUrl, imageUrl)
        }
    } else {
        loadImages(callback, thumbnailUrl, imageUrl)
    }
}

fun ImageView.loadImages(callback: ImageLoadCallback?, vararg urls: String) {
    safelyLoadImage(LinkedList(urls.toList()), callback)
}

/**
 * Loads an image into the [android.widget.ImageView] by attempting each URL in the [java.util.Queue]
 * until one is successful. Applies fit/center crop and will load a [androidx.palette.graphics.Palette].
 */
private fun ImageView.safelyLoadImage(imageUrls: Queue<String>, callback: ImageLoadCallback?) {
    // Attempt to load 1) the URL saved as a tag to this ImageView or 2) the next URL in the queue. If this URL fails, recursively call this to
    // attempt to load the next URL. Signal a failure if the queue empties before successfully loading a URL.
    var url: String? = null
    val savedUrl = getTag(R.id.url) as String?
    if (savedUrl?.isNotBlank() == true) {
        if (imageUrls.contains(savedUrl)) {
            url = savedUrl
        } else {
            setTag(R.id.url, null)
        }
    }
    if (url == null) {
        do {
            val polledUrl = imageUrls.poll()
            if (polledUrl == null) {
                callback?.onFailedImageLoad()
                return
            } else {
                url = polledUrl
            }
        } while (url.isNullOrBlank())
    }
    if (url.isNullOrEmpty()) {
        callback?.onFailedImageLoad()
        return
    }
    val imageUrl = url
    Picasso.with(context)
        .load(imageUrl.ensureHttpsScheme())
        .transform(PaletteTransformation.instance())
        .into(this, object : Callback {
            override fun onSuccess() {
                setTag(R.id.url, imageUrl)
                callback?.onSuccessfulImageLoad(PaletteTransformation.getPalette((drawable as BitmapDrawable).bitmap))
            }

            override fun onError() {
                safelyLoadImage(imageUrls, callback)
            }
        })
}

/**
 * Loads the URL into an ImageView, centering and fitting it into the image. If the URL appears to be for the same image, no placeholder or fade
 * animation is shown.
 */
fun ImageView.loadThumbnail(imageUrl: String?, @DrawableRes errorResId: Int = R.drawable.thumbnail_image_empty, callback: ImageLoadCallback? = null) {
    val tag = getTag(R.id.image)
    val isSameImage = tag != null && tag == imageUrl?.getImageId()
    val requestCreator = Picasso.with(context)
        .load(imageUrl.ensureHttpsScheme())
        .error(errorResId)
        .fit()
        .centerCrop()
    if (isSameImage) {
        requestCreator.noFade().noPlaceholder()
    } else {
        requestCreator.placeholder(errorResId).noFade()
    }
    requestCreator.into(this, object : Callback {
        override fun onSuccess() {
            setTag(R.id.url, imageUrl)
            callback?.onSuccessfulImageLoad(PaletteTransformation.getPalette((drawable as BitmapDrawable).bitmap))
        }

        override fun onError() {
            callback?.onFailedImageLoad()
        }
    })
}

/**
 * Loads the URL into an ImageView, centering and fitting it into the image. Always shows the fade animation (and it faster for that).
 */
fun ImageView.loadThumbnailInList(imageUrl: String?, @DrawableRes errorResId: Int = R.drawable.thumbnail_image_empty) {
    Picasso.with(context)
        .load(imageUrl.ensureHttpsScheme())
        .placeholder(errorResId)
        .error(errorResId)
        .fit()
        .centerCrop()
        .into(this)
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
            loadThumbnails(response.images.small.url, *imageId.createImageUrls().toTypedArray())
        } catch (e: Exception) {
            loadThumbnails(*imageId.createImageUrls().toTypedArray())
        }
    } else {
        loadThumbnails(*imageId.createImageUrls().toTypedArray())
    }
}

private fun ImageView.loadThumbnails(vararg urls: String) {
    safelyLoadThumbnail(LinkedList(urls.toList()))
}

private fun ImageView.safelyLoadThumbnail(imageUrls: Queue<String>) {
    var url: String? = null
    while (url.isNullOrEmpty() && imageUrls.isNotEmpty()) {
        url = imageUrls.poll()
    }
    val imageUrl = url
    Picasso.with(context)
        .load(imageUrl.ensureHttpsScheme())
        .placeholder(R.drawable.thumbnail_image_empty)
        .error(R.drawable.thumbnail_image_empty)
        .fit()
        .centerCrop()
        .into(this, object : Callback {
            override fun onSuccess() {
            }

            override fun onError() {
                safelyLoadThumbnail(imageUrls)
            }
        })
}

fun Int.createImageUrls(): List<String> {
    val imageUrlPrefix = "https://cf.geekdo-images.com/images/pic"
    return listOf("$imageUrlPrefix$this.jpg", "$imageUrlPrefix$this.png")
}

fun ImageView.setOrClearColorViewValue(color: Int, disabled: Boolean = false) {
    if (color != Color.TRANSPARENT) {
        setColorViewValue(color, disabled)
    } else {
        setImageDrawable(null)
    }
}

fun ImageView.setColorViewValue(color: Int, disabled: Boolean = false) {
    val colorChoiceDrawable = drawable as? GradientDrawable ?: GradientDrawable().apply {
        shape = GradientDrawable.OVAL
    }

    if (disabled) {
        colorChoiceDrawable.colors = if (color.isColorDark()) {
            intArrayOf(Color.WHITE, color)
        } else {
            intArrayOf(color, Color.BLACK)
        }
    } else {
        colorChoiceDrawable.setColor(color)
    }

    colorChoiceDrawable.setStroke(
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, resources.displayMetrics).toInt(),
        color.darkenColor()
    )

    setImageDrawable(colorChoiceDrawable)
}

/**
 * Create an ImageView that is a small circle, useful for showing a color.
 */
fun Context.createSmallCircle(): ImageView {
    val size = resources.getDimensionPixelSize(R.dimen.color_circle_diameter_small)
    val view = ImageView(this)
    view.layoutParams = LinearLayout.LayoutParams(size, size).apply {
        setMargins(resources.getDimensionPixelSize(R.dimen.color_circle_diameter_small_margin))
    }
    return view
}
