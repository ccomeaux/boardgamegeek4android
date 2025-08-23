package com.boardgamegeek.extensions

import android.graphics.Color
import androidx.annotation.ColorInt
import java.util.Locale

@ColorInt
fun @receiver:ColorInt Int.darkenColor(ratio: Double = 0.5): Int {
    return if (this == Color.TRANSPARENT) {
        Color.argb(127, 127, 127, 127)
    } else blendWith(Color.BLACK, ratio)
}

@ColorInt
fun @receiver:ColorInt Int.getTextColor(transparentColor: Int = Color.BLACK): Int {
    return if (this == Color.TRANSPARENT)
        transparentColor
    else if (this.isColorDark())
        Color.WHITE
    else
        Color.BLACK
}

/**
 * Calculate whether a color is light or dark, based on a commonly known brightness formula.
 *
 * http://en.wikipedia.org/wiki/HSV_color_space%23Lightness
 */
fun @receiver:ColorInt Int.isColorDark(): Boolean {
    return (299 * Color.red(this) + 587 * Color.green(this) + 114 * Color.blue(this)) / 1000f < 128
}

fun Int.blendWith(color: Int, ratio: Double): Int {
    val ir = 1.0 - ratio

    val a = (Color.alpha(this) * ratio + Color.alpha(color) * ir).toInt()
    val r = (Color.red(this) * ratio + Color.red(color) * ir).toInt()
    val g = (Color.green(this) * ratio + Color.green(color) * ir).toInt()
    val b = (Color.blue(this) * ratio + Color.blue(color) * ir).toInt()

    return Color.argb(a, r, g, b)
}

fun String?.isKnownColor(): Boolean {
    return if (this == null) false else BggColors.colorNameMap.containsKey(this.formatColorKey())
}

fun String?.asColorRgb(): Int {
    return when {
        this == null -> Color.TRANSPARENT
        isBlank() -> Color.TRANSPARENT
        this[0] == '#' -> if (length == 7 || length == 9) {
            // Use a long to avoid rollovers on #ffXXXXXX
            var color = substring(1).toLong(16)
            if (length == 7) {
                // Set the alpha value
                color = color or -0x1000000
            }
            color.toInt()
        } else {
            Color.TRANSPARENT
        }
        else -> BggColors.colorNameMap[this.formatColorKey()] ?: Color.TRANSPARENT
    }
}

internal fun String.formatColorKey() = lowercase(Locale.US)

/**
 * Static methods for modifying and applying colors to views.
 */
object BggColors {
    val standardColorList = listOf(
        "Red" to 0xFF_FF_00_00.toInt(),
        "Yellow" to 0xFF_FF_FF_00.toInt(),
        "Blue" to 0xFF_00_00_FF.toInt(),
        "Green" to 0xFF_00_80_00.toInt(),
        "Purple" to 0xFF_80_00_80.toInt(),
        "Orange" to 0xFF_E5_94_00.toInt(),
        "White" to 0xFF_FF_FF_FF.toInt(),
        "Black" to 0xFF_00_00_00.toInt(),
        "Natural" to 0xFF_E9_C2_A6.toInt(),
        "Brown" to 0xFF_A5_2A_2A.toInt(),
    )

    val colorList = standardColorList + listOf(
        "Tan" to 0xFF_DB_93_70.toInt(),
        "Gray" to 0xFF_88_88_88.toInt(),
        "Gold" to 0xFF_FF_D7_00.toInt(),
        "Silver" to 0xFF_C0_C0_C0.toInt(),
        "Bronze" to 0xFF_8C_78_53.toInt(),
        "Ivory" to 0xFF_FF_FF_F0.toInt(),
        "Rose" to 0xFF_FF_00_7F.toInt(),
        "Pink" to 0xFF_CD_91_9E.toInt(),
        "Teal" to 0xFF_00_80_80.toInt(),
        // "Light Gray" to 0xFF_CC_CC_CC,
        // "Dark Gray" to 0xFF_44_44_44,
        // "Cyan" = 0xFF_00_FF_FF,
        // "Magenta"" = 0xFF_FF_00_FF,
        // "Aqua" to 0xFF_66_CC_CC,
    )

    val ratingColors = listOf(
        -0x10000, // 1
        -0xcc9a, // 2
        -0x9967, // 3
        -0x9934, // 4
        -0x336601, // 5
        -0x666601, // 6
        -0x660001, // 7
        -0x990067, // 8
        -0xcc3367, // 9
        -0xff3400, // 10
    )

    val fiveStageColors = listOf(
        -0xdb6a9d, // 0xFFDB303B - alternate red color
        -0xd03b7e,
        -0xe27533,
        -0xac965e,
        -0x20b8af,
    )

    /** 12 contrasting colors useful in charts showing up to 12 data points */
    val twelveStageColors = listOf(
        -0x201434,
        -0x442333,
        -0x673433,
        -0x874331,
        -0x9f5736,
        -0xad6f46,
        -0xba8957,
        -0xc7a167,
        -0xd4bb77,
        -0xe1d386,
        -0xe7de9f,
        -0xede7b8,
    )

    internal val colorNameMap
        get() = colorList.associate { it.first.formatColorKey() to it.second }
}
