package com.boardgamegeek.extensions

import android.content.Context
import android.graphics.Color
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.palette.graphics.Palette
import androidx.palette.graphics.Target
import com.boardgamegeek.R

/**
 * Gets a swatch from the palette suitable for tinting icon images.
 */
fun Palette?.getIconSwatch(): Palette.Swatch {
    return this?.darkVibrantSwatch
            ?: this?.vibrantSwatch
            ?: this?.swatches?.getOrNull(0)
            ?: Palette.Swatch(Color.BLACK, 0)
}

/**
 * Gets a swatch from the palette suitable for light text.
 */
fun Palette?.getDarkSwatch(): Palette.Swatch {
    return this?.darkMutedSwatch
            ?: this?.darkVibrantSwatch
            ?: this?.swatches?.getOrNull(0)
            ?: Palette.Swatch(Color.BLACK, 0)
}

private val winsTargets = arrayOf(Target.VIBRANT, Target.LIGHT_VIBRANT, Target.DARK_VIBRANT, Target.LIGHT_MUTED, Target.MUTED, Target.DARK_MUTED)

private val winnablePlaysTargets = arrayOf(Target.DARK_VIBRANT, Target.DARK_MUTED, Target.MUTED, Target.VIBRANT, Target.LIGHT_VIBRANT, Target.LIGHT_MUTED)

private val allPlaysTargets = arrayOf(Target.LIGHT_MUTED, Target.LIGHT_VIBRANT, Target.MUTED, Target.VIBRANT, Target.DARK_MUTED, Target.DARK_VIBRANT)

fun Palette.getPlayCountColors(context: Context): IntArray {
    val winColor = getColor(winsTargets, context, R.color.orange)
    val winnablePlaysColor = getColor(winnablePlaysTargets, context, R.color.dark_blue, winColor.second)
    val allPlaysColor = getColor(allPlaysTargets, context, R.color.light_blue, winColor.second, winnablePlaysColor.second)

    return intArrayOf(winColor.first, winnablePlaysColor.first, allPlaysColor.first)
}

private fun Palette.getColor(targets: Array<Target>, context: Context, @ColorRes defaultColorResId: Int, vararg usedTargets: Target): Pair<Int, Target> {
    for (target in targets) {
        if (!usedTargets.contains(target)) {
            val swatch = getSwatchForTarget(target)
            if (swatch != null) return swatch.rgb to target
        }
    }
    return ContextCompat.getColor(context, defaultColorResId) to Target.Builder().build()
}