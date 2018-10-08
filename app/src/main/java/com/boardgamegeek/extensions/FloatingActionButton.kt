package com.boardgamegeek.extensions

import android.content.res.ColorStateList
import android.graphics.Color
import com.google.android.material.floatingactionbutton.FloatingActionButton

fun FloatingActionButton.colorize(color: Int): Boolean {
    if (color != Color.TRANSPARENT &&
            backgroundTintList != ColorStateList.valueOf(color)) {
        backgroundTintList = ColorStateList.valueOf(color)
        return true
    }
    return false
}

fun FloatingActionButton.ensureShown() {
    postDelayed({ show() }, 2000)
}