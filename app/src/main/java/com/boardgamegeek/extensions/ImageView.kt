package com.boardgamegeek.extensions

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.view.setMargins
import androidx.palette.graphics.Palette
import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.util.FileUtils
import com.boardgamegeek.util.PaletteTransformation
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
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
fun ImageView.loadImage(url: String?, @DrawableRes errorResId: Int = 0, callback: ImageLoadCallback? = null) {
    safelyLoadImage(LinkedList(listOf(url)), errorResId, callback)
}

fun ImageView.loadImage(urls: List<String>, @DrawableRes errorResId: Int = 0, callback: ImageLoadCallback? = null) {
    safelyLoadImage(LinkedList(urls.filter { it.isNotBlank() }), errorResId, callback)
}

/**
 * Loads an image into the [android.widget.ImageView] by attempting each URL in the [java.util.Queue]
 * until one is successful. Applies fit/center crop and will load a [androidx.palette.graphics.Palette].
 */
private fun ImageView.safelyLoadImage(imageUrls: Queue<String>?, @DrawableRes errorResId: Int = 0, callback: ImageLoadCallback? = null) {
    // Attempt to load 1) the URL saved as a tag to this ImageView or 2) the next URL in the queue. If this URL fails, recursively call this to
    // attempt to load the next URL. Signal a failure if the queue empties before successfully loading a URL.
    var url: String?
    do {
        val polledUrl = imageUrls?.poll()
        if (polledUrl == null) {
            callback?.onFailedImageLoad()
            return
        } else {
            url = polledUrl
        }
    } while (url.isNullOrBlank())
    if (url.isEmpty()) {
        callback?.onFailedImageLoad()
        return
    }
    val imageUrl = url
    val isSameImage = getTag(R.id.image) == imageUrl.getImageId()
    val requestCreator = Picasso.with(context)
        .load(url.ensureHttpsScheme())
        .transform(PaletteTransformation.instance())
    if (isSameImage) {
        requestCreator.noFade().noPlaceholder()
    } else if (errorResId != 0) {
        requestCreator.placeholder(errorResId)
    }
    requestCreator.into(this, object : Callback {
        override fun onSuccess() {
            setTag(R.id.image, imageUrl.getImageId())
            callback?.onSuccessfulImageLoad(PaletteTransformation.getPalette((drawable as BitmapDrawable).bitmap))
        }

        override fun onError() {
            this@safelyLoadImage.safelyLoadImage(imageUrls, errorResId, callback)
        }
    })
}

/**
 * Loads the URL into an ImageView, centering and fitting it into the image. If the URL appears to be for the same image, no placeholder is shown.
 */
fun ImageView.loadThumbnail(imageUrl: String?, @DrawableRes errorResId: Int = R.drawable.thumbnail_image_empty, callback: ImageLoadCallback? = null) {
    val requestCreator = Picasso.with(context)
        .load(imageUrl.ensureHttpsScheme())
        .error(errorResId)
        .fit()
        .centerCrop()
        .noFade()
        .placeholder(errorResId)
    requestCreator.into(this, object : Callback {
        override fun onSuccess() {
            callback?.onSuccessfulImageLoad(PaletteTransformation.getPalette((drawable as BitmapDrawable).bitmap))
        }

        override fun onError() {
            callback?.onFailedImageLoad()
        }
    })
}

fun ImageView.loadThumbnail(vararg urls: String, saveBitmap: Boolean = false) {
    safelyLoadThumbnail(LinkedList(urls.toList()), saveBitmap)
}

private fun ImageView.safelyLoadThumbnail(imageUrls: Queue<String>, saveBitmap: Boolean = false) {
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
                if (saveBitmap) {
                    imageUrl?.let {
                        Picasso.with(context).load(imageUrl.ensureHttpsScheme()).into(object : Target {
                            override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                                if (bitmap != null) {
                                    FileUtils.getFile(context, BggContract.PATH_THUMBNAILS, it)?.let { file ->
                                        FileUtils.saveBitmap(file, bitmap)
                                    }
                                }
                            }

                            override fun onBitmapFailed(errorDrawable: Drawable?) {
                            }

                            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
                            }
                        })
                    }
                }
            }

            override fun onError() {
                safelyLoadThumbnail(imageUrls)
            }
        })
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
