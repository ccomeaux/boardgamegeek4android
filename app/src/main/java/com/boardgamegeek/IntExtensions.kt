package com.boardgamegeek

import com.boardgamegeek.util.MathUtils

fun Int.constrain(min: Int, max: Int): Int {
    return MathUtils.constrain(this, min, max)
}