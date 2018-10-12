package com.boardgamegeek.extensions

import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.TypedValue
import android.widget.ImageView
import androidx.annotation.ColorInt
import com.boardgamegeek.R
import com.boardgamegeek.util.HttpUtils
import com.boardgamegeek.util.ImageUtils
import com.boardgamegeek.util.PaletteTransformation
import com.squareup.picasso.Picasso

fun ImageView.setOrClearColorFilter(@ColorInt color: Int) {
    if (color == Color.TRANSPARENT) clearColorFilter() else setColorFilter(color)
}

fun ImageView.loadUrl(url: String, callback: ImageUtils.Callback? = null) {
    val isSameImage = getTag(R.id.image) == ImageUtils.getImageId(url)
    val requestCreator = Picasso.with(context)
            .load(if (url.isEmpty()) null else HttpUtils.ensureScheme(url))
            .transform(PaletteTransformation.instance())
    if (isSameImage) {
        requestCreator.noFade().noPlaceholder()
    }
    requestCreator
            .into(this, object : com.squareup.picasso.Callback {
                override fun onSuccess() {
                    setTag(R.id.image, ImageUtils.getImageId(url))
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

fun ImageView.setColorViewValue(color: Int, disabled: Boolean = false) {
    val colorChoiceDrawable = drawable as? GradientDrawable ?: GradientDrawable().apply {
        shape = GradientDrawable.OVAL
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && disabled) {
        colorChoiceDrawable.colors = intArrayOf(color, Color.DKGRAY)
    } else {
        colorChoiceDrawable.setColor(color)
    }

    colorChoiceDrawable.setStroke(
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, resources.displayMetrics).toInt(),
            color.darkenColor())

    setImageDrawable(colorChoiceDrawable)
}
