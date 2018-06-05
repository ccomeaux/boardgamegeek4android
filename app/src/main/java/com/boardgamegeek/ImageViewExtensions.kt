package com.boardgamegeek

import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.support.annotation.ColorInt
import android.widget.ImageView
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
            .load(HttpUtils.ensureScheme(url))
            .transform(PaletteTransformation.instance())
    if (isSameImage){
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
