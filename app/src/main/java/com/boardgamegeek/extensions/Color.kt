package com.boardgamegeek.extensions

import android.graphics.Color
import androidx.annotation.ColorInt
import com.boardgamegeek.util.ColorUtils
import java.util.*

@ColorInt
fun @receiver:ColorInt Int.darkenColor(): Int {
    return if (this == Color.TRANSPARENT) {
        Color.argb(127, 127, 127, 127)
    } else Color.rgb(
            Color.red(this) * 192 / 256,
            Color.green(this) * 192 / 256,
            Color.blue(this) * 192 / 256)
}

@ColorInt
fun @receiver:ColorInt Int.getTextColor(): Int {
    return if (this != Color.TRANSPARENT && this.isColorDark())
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
    return (30 * Color.red(this) + 59 * Color.green(this) + 11 * Color.blue(this)) / 100 <= 130
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
    return if (this == null) false else ColorUtils.colorNameMap.containsKey(formatKey(this))
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
        else -> ColorUtils.colorNameMap.getOrDefault(formatKey(this), Color.TRANSPARENT)
    }
}

private fun formatKey(name: String): String {
    return name.toLowerCase(Locale.US)
}

val ratingColors = intArrayOf(
        -0x10000, // 1
        -0xcc9a, // 2
        -0x9967, // 3
        -0x9934, // 4
        -0x336601, // 5
        -0x666601, // 6
        -0x660001, // 7
        -0x990067, // 8
        -0xcc3367, // 9
        -0xff3400 // 10
)

val fiveStageColors = intArrayOf(-0xdb6a9d, -0xd03b7e, -0xe27533, -0xac965e, -0x20b8af)// 0xFFDB303B - alternate red color
val twelveStageColors = intArrayOf(-0x201434, -0x442333, -0x673433, -0x874331, -0x9f5736, -0xad6f46, -0xba8957, -0xc7a167, -0xd4bb77, -0xe1d386, -0xe7de9f, -0xede7b8)
