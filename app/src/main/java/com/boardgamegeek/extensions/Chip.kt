package com.boardgamegeek.extensions

import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import com.google.android.material.chip.Chip
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import com.squareup.picasso.Transformation
import kotlin.math.min


fun Chip.loadIcon(imageUrl: String?, @DrawableRes errorResId: Int = 0) {
    val creator = Picasso.with(context)
        .load(imageUrl.ensureHttpsScheme())
        .resize(chipIconSize.toInt(), chipIconSize.toInt())
        .centerCrop()
        .transform(CircleTransform())
    if (errorResId != 0) {
        creator
            .error(errorResId)
            .placeholder(errorResId)
    }
    creator.into(object : Target {
        override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
            if (errorResId != 0)
                this@loadIcon.setChipIconResource(errorResId)
        }

        override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
            this@loadIcon.chipIcon = BitmapDrawable(resources, bitmap)
        }

        override fun onBitmapFailed(errorDrawable: Drawable?) {
            if (errorResId != 0)
                this@loadIcon.setChipIconResource(errorResId)
        }
    })
}

class CircleTransform : Transformation {
    override fun transform(source: Bitmap): Bitmap {
        val size = min(source.width, source.height)

        val x = (source.width - size) / 2
        val y = (source.height - size) / 2

        val squaredBitmap = Bitmap.createBitmap(source, x, y, size, size)
        if (squaredBitmap != source) {
            source.recycle()
        }

        val bitmap = Bitmap.createBitmap(size, size, source.config ?: Bitmap.Config.ARGB_8888)

        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            shader = BitmapShader(squaredBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            isAntiAlias = true
        }

        val r = size / 2f
        canvas.drawCircle(r, r, r, paint)

        squaredBitmap.recycle()
        return bitmap
    }

    override fun key(): String {
        return "circle"
    }
}
