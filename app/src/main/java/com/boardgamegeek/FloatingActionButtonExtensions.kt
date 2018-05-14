package com.boardgamegeek

import android.content.res.ColorStateList
import android.graphics.Color
import android.support.design.widget.FloatingActionButton

fun FloatingActionButton.colorize(color: Int) {
    if (color != Color.TRANSPARENT) {
        backgroundTintList = ColorStateList.valueOf(color)
    }
}
