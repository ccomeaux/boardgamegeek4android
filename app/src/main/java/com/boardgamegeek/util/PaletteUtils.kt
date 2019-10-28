package com.boardgamegeek.util

import android.graphics.Color
import androidx.palette.graphics.Palette

/**
 * Helper class for dealing with [androidx.palette.graphics.Palette].
 */
object PaletteUtils {
    /**
     * Gets a swatch from the palette suitable for tinting icon images.
     */
    @JvmStatic
    fun getIconSwatch(palette: Palette?): Palette.Swatch {
        return palette?.darkVibrantSwatch
                ?: palette?.vibrantSwatch
                ?: palette?.swatches?.getOrNull(0)
                ?: Palette.Swatch(Color.BLACK, 0)
    }

    /**
     * Gets a swatch from the palette best suited for header text.
     */
    @JvmStatic
    fun getHeaderSwatch(palette: Palette?): Palette.Swatch {
        return palette?.vibrantSwatch
                ?: palette?.darkMutedSwatch
                ?: palette?.swatches?.getOrNull(0)
                ?: Palette.Swatch(Color.BLACK, 0)
    }
}
