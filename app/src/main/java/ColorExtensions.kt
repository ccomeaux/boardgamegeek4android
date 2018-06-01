import android.graphics.Color
import android.support.annotation.ColorInt

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