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
import com.boardgamegeek.R
import com.boardgamegeek.util.ImageUtils
import com.boardgamegeek.util.ImageUtils.getImageId
import com.boardgamegeek.util.PaletteTransformation
import com.squareup.picasso.Picasso

fun ImageView.setOrClearColorFilter(@ColorInt color: Int) {
    if (color == Color.TRANSPARENT) clearColorFilter() else setColorFilter(color)
}

fun ImageView.loadUrl(url: String?, callback: ImageUtils.Callback? = null) {
    val isSameImage = getTag(R.id.image) == url?.getImageId()
    val requestCreator = Picasso.with(context)
            .load(url.ensureHttpsScheme())
            .transform(PaletteTransformation.instance())
    if (isSameImage) {
        requestCreator.noFade().noPlaceholder()
    }
    requestCreator
            .into(this, object : com.squareup.picasso.Callback {
                override fun onSuccess() {
                    setTag(R.id.image, url?.getImageId())
                    if (callback != null) {
                        val bitmap = (drawable as BitmapDrawable).bitmap
                        val palette = PaletteTransformation.getPalette(bitmap)
                        callback.onSuccessfulImageLoad(palette)
                    }
                }

                override fun onError() {
                    callback?.onFailedImageLoad()
                }
            })
}

fun ImageView.loadThumbnail(imageUrl: String?, @DrawableRes errorResId: Int = R.drawable.thumbnail_image_empty) {
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
        requestCreator.placeholder(errorResId)
    }
    requestCreator.into(this, object : com.squareup.picasso.Callback {
        override fun onSuccess() {
            setTag(R.id.url, imageUrl)
        }

        override fun onError() {
        }
    })
}

fun ImageView.loadThumbnailInList(imageUrl: String?, @DrawableRes errorResId: Int = R.drawable.thumbnail_image_empty) {
    Picasso.with(context)
            .load(imageUrl.ensureHttpsScheme())
            .placeholder(errorResId)
            .error(errorResId)
            .fit()
            .centerCrop()
            .into(this)
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
            color.darkenColor())

    setImageDrawable(colorChoiceDrawable)
}

fun Context.createSmallCircle(): ImageView {
    val size = resources.getDimensionPixelSize(R.dimen.color_circle_diameter_small)
    val margin = resources.getDimensionPixelSize(R.dimen.color_circle_diameter_small_margin)
    val view = ImageView(this)
    view.layoutParams = LinearLayout.LayoutParams(size, size).apply {
        setMargins(margin)
    }
    return view
}
