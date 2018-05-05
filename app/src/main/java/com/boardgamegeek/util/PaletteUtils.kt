package com.boardgamegeek.util

import android.content.Context
import android.graphics.Color
import android.support.annotation.ColorInt
import android.support.v4.content.ContextCompat
import android.support.v7.graphics.Palette
import android.support.v7.graphics.Target
import android.util.Pair
import android.widget.ImageView
import android.widget.TextView
import butterknife.ButterKnife
import com.boardgamegeek.R

/**
 * Helper class for dealing with [android.support.v7.graphics.Palette].
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
     * Gets a swatch from the palette suitable for light text.
     */
    @JvmStatic
    fun getDarkSwatch(palette: Palette?): Palette.Swatch {
        return palette?.darkMutedSwatch
                ?: palette?.darkVibrantSwatch
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

    private val winsTargets = arrayOf(Target.VIBRANT, Target.LIGHT_VIBRANT, Target.DARK_VIBRANT, Target.LIGHT_MUTED, Target.MUTED, Target.DARK_MUTED)

    private val winnablePlaysTargets = arrayOf(Target.DARK_VIBRANT, Target.DARK_MUTED, Target.MUTED, Target.VIBRANT, Target.LIGHT_VIBRANT, Target.LIGHT_MUTED)

    private val allPlaysTargets = arrayOf(Target.LIGHT_MUTED, Target.LIGHT_VIBRANT, Target.MUTED, Target.VIBRANT, Target.DARK_MUTED, Target.DARK_VIBRANT)

    @JvmStatic
    fun getPlayCountColors(palette: Palette, context: Context): IntArray {
        val winColor = getColor(palette, winsTargets, ContextCompat.getColor(context, R.color.orange))
        val winnablePlaysColor = getColor(palette, winnablePlaysTargets, ContextCompat.getColor(context, R.color.dark_blue), winColor.second)
        val allPlaysColor = getColor(palette, allPlaysTargets, ContextCompat.getColor(context, R.color.light_blue), winColor.second, winnablePlaysColor.second)

        return intArrayOf(winColor.first, winnablePlaysColor.first, allPlaysColor.first)
    }

    private fun getColor(palette: Palette, targets: Array<Target>, @ColorInt defaultColor: Int, vararg usedTargets: Target): Pair<Int, Target> {
        for (target in targets) {
            if (!usedTargets.contains(target)) {
                val swatch = palette.getSwatchForTarget(target)
                if (swatch != null) return Pair.create(swatch.rgb, target)
            }
        }
        return Pair.create(defaultColor, Target.Builder().build())
    }
}
