package com.boardgamegeek.extensions

import android.content.res.ColorStateList
import android.graphics.Color
import androidx.annotation.ColorInt
import com.google.android.material.floatingactionbutton.FloatingActionButton

fun FloatingActionButton.colorize(@ColorInt color: Int): Boolean {
    val colorStateList = ColorStateList.valueOf(color)
    if (color != Color.TRANSPARENT && backgroundTintList != colorStateList) {
        backgroundTintList = colorStateList
        colorImageTint(color)
        return true
    }
    return false
}

private fun FloatingActionButton.colorImageTint(backgroundColor: Int) {
    imageTintList = ColorStateList.valueOf(
        if (backgroundColor.isColorDark()) {
            Color.WHITE
        } else {
            Color.BLACK
        }
    )
}

fun FloatingActionButton.ensureShown() {
    postDelayed({ show() }, 2000)
}
