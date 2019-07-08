package com.boardgamegeek.util

import android.graphics.Bitmap

import com.squareup.picasso.Transformation
import java.util.WeakHashMap

import androidx.palette.graphics.Palette

/**
 * Used to get Picasso and Palette to play well with each other.
 */
class PaletteTransformation private constructor() : Transformation {

    override fun transform(source: Bitmap): Bitmap {
        val palette = Palette.from(source).generate()
        CACHE[source] = palette
        return source
    }

    override fun key(): String {
        return "" // Stable key for all requests. An unfortunate requirement.
    }

    companion object {
        private val INSTANCE = PaletteTransformation()
        private val CACHE = WeakHashMap<Bitmap, Palette>()

        fun instance(): PaletteTransformation {
            return INSTANCE
        }

        fun getPalette(bitmap: Bitmap): Palette? {
            return CACHE[bitmap]
        }
    }
}
