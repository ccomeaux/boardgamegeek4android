@file:JvmName("FloatingActionButtonUtils")

package com.boardgamegeek.extensions

import android.annotation.TargetApi
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import com.google.android.material.floatingactionbutton.FloatingActionButton

fun FloatingActionButton.colorize(color: Int): Boolean {
    val colorStateList = ColorStateList.valueOf(color)
    if (color != Color.TRANSPARENT && backgroundTintList != colorStateList) {
        backgroundTintList = colorStateList
        colorImageTint(color)
        return true
    }
    return false
}

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
private fun FloatingActionButton.colorImageTint(backgroundColor: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        imageTintList = ColorStateList.valueOf(
                if (backgroundColor.isColorDark()) {
                    Color.WHITE
                } else {
                    Color.BLACK
                }
        )
    }
}

fun FloatingActionButton.ensureShown() {
    postDelayed({ show() }, 2000)
}
