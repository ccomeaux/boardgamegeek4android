package com.boardgamegeek.util

import android.graphics.Bitmap

import com.squareup.picasso.Transformation
import java.util.WeakHashMap

import androidx.palette.graphics.Palette
import androidx.palette.graphics.Target

/**
 * Used to get Picasso and Palette to play well with each other.
 */
class PaletteTransformation private constructor() : Transformation {
    override fun transform(source: Bitmap): Bitmap {
        val palette = Palette.from(source).addTarget(DARK_TARGET).generate()
        CACHE[source] = palette
        return source
    }

    override fun key(): String = "" // Stable key for all requests. An unfortunate requirement.

    companion object {
        private val INSTANCE = PaletteTransformation()
        private val CACHE = WeakHashMap<Bitmap, Palette>()
        fun instance() = INSTANCE
        fun getPalette(bitmap: Bitmap) = CACHE[bitmap]
    }
}

val DARK_TARGET = Target.Builder()
    .setTargetLightness(0.26f)
    .setMaximumLightness(0.45f)
    .setExclusive(false)
    .build()
