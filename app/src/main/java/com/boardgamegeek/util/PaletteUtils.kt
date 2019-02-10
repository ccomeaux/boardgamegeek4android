package com.boardgamegeek.util

import android.graphics.Color
import android.widget.ImageView
import android.widget.TextView
import androidx.palette.graphics.Palette
import butterknife.ButterKnife

/**
 * Helper class for dealing with [androidx.palette.graphics.Palette].
 */
object PaletteUtils {
    /**
     * Sets the [android.widget.TextView]'s text color filter to the RGB color.
     */
    @JvmStatic
    val rgbTextViewSetter: ButterKnife.Setter<TextView, Int?> = ButterKnife.Setter { view, value, _ ->
        if (value != null) view.setTextColor(value)
    }

    /**
     * Sets the [android.widget.ImageView]'s color filter to the RGB color.
     */
    @JvmStatic
    val rgbIconSetter: ButterKnife.Setter<ImageView, Int?> = ButterKnife.Setter { view, value, _ ->
        if (value != null) view.setColorFilter(value)
    }

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
