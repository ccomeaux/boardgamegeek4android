package com.boardgamegeek.extensions

import android.content.Context
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.palette.graphics.Palette
import androidx.palette.graphics.Target
import androidx.palette.graphics.get
import com.boardgamegeek.R
import com.boardgamegeek.util.DARK_TARGET

/**
 * Gets a color from the palette suitable for tinting icon images and header text.
 */
@ColorInt
fun Palette?.getIconColor(): Int {
    return (this?.darkVibrantSwatch
        ?: this?.vibrantSwatch
        ?: this?.getSwatchForTarget(DARK_TARGET)
        ?: this?.swatches?.minByOrNull { it.hsl.last() })?.rgb
        ?: Color.BLACK
}

/**
 * Gets a color from the palette suitable as a background for light text.
 */
@ColorInt
fun Palette?.getDarkColor(): Int {
    return (this?.darkMutedSwatch
        ?: this?.darkVibrantSwatch
        ?: this?.getSwatchForTarget(DARK_TARGET)
        ?: this?.swatches?.minByOrNull { it.hsl.last() })?.rgb
        ?: Color.BLACK
}

private val winsTargets = arrayOf(Target.VIBRANT, Target.LIGHT_VIBRANT, Target.DARK_VIBRANT, Target.LIGHT_MUTED, Target.MUTED, Target.DARK_MUTED)

private val winnablePlaysTargets =
    arrayOf(Target.DARK_VIBRANT, Target.DARK_MUTED, Target.MUTED, Target.VIBRANT, Target.LIGHT_VIBRANT, Target.LIGHT_MUTED)

private val allPlaysTargets = arrayOf(Target.LIGHT_MUTED, Target.LIGHT_VIBRANT, Target.MUTED, Target.VIBRANT, Target.DARK_MUTED, Target.DARK_VIBRANT)

fun Palette.getPlayCountColors(context: Context): IntArray {
    val winColor = getColor(winsTargets, context, R.color.orange)
    val winnablePlaysColor = getColor(winnablePlaysTargets, context, R.color.dark_blue, winColor.second)
    val allPlaysColor = getColor(allPlaysTargets, context, R.color.light_blue, winColor.second, winnablePlaysColor.second)

    return intArrayOf(winColor.first, winnablePlaysColor.first, allPlaysColor.first)
}

private fun Palette.getColor(
    targets: Array<Target>,
    context: Context,
    @ColorRes defaultColorResId: Int,
    vararg usedTargets: Target
): Pair<Int, Target> {
    return targets.find { !usedTargets.contains(it) && this[it] != null }?.let {
        this[it]!!.rgb to it
    } ?: (ContextCompat.getColor(context, defaultColorResId) to Target.Builder().build())
}
