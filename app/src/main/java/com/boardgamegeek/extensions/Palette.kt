package com.boardgamegeek.extensions

import android.graphics.Color
import android.support.v7.graphics.Palette

fun Palette?.getIconSwatch(): Palette.Swatch {
    return this?.darkVibrantSwatch
            ?: this?.vibrantSwatch
            ?: this?.swatches?.getOrNull(0)
            ?: Palette.Swatch(Color.BLACK, 0)
}