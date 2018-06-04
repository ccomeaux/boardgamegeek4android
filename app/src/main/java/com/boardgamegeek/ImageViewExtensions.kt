package com.boardgamegeek

import android.graphics.Color
import android.support.annotation.ColorInt
import android.widget.ImageView

fun ImageView.setOrClearColorFilter(@ColorInt color: Int) {
    if (color == Color.TRANSPARENT) clearColorFilter() else setColorFilter(color)
}
